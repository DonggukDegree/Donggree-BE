package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.command.RequirementSetCommand;
import com.donggree.curriculum.internal.application.command.RequirementSetUpdateCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.college.College;
import com.donggree.curriculum.internal.domain.college.CollegeRepository;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 졸업 요건 세트(requirement_set) 변경(Command) 응용 서비스.
 * 세트 1건을 단위로 생성·수정하며, 연결 졸업 규칙은 선택/해제 결과 전체를 받아 통째로 교체한다.
 */
@Service
@RequiredArgsConstructor
public class RequirementSetCommandService {

    private final RequirementSetRepository requirementSetRepository;
    private final GraduationRuleRepository graduationRuleRepository;
    private final DepartmentRepository departmentRepository;
    private final CollegeRepository collegeRepository;

    /**
     * 새 졸업 요건 세트를 생성하고 선택한 졸업 규칙들을 연결한다.
     * 버전은 (학과, 적용년도) lineage의 최신 버전+1로 자동 채번하며, 활성으로 등록할 경우 적용년도가 겹치는 기존 활성 세트가 없어야 한다.
     */
    @Transactional
    public Long create(RequirementSetCommand command) {
        Long collegeId = resolveOrCreateCollege(command.collegeName());
        Long departmentId = resolveOrCreateDepartment(command.departmentName(), collegeId);
        validateActiveNotOverlapping(departmentId, command.yearStart(), command.yearEnd(), command.active(), null);
        int version = nextVersion(departmentId, command.yearStart(), command.yearEnd());
        List<GraduationRule> rules = loadRules(command.graduationRuleIds());

        RequirementSet set = RequirementSet.create(
                departmentId,
                command.yearStart(),
                command.yearEnd(),
                version,
                command.description(),
                command.sheetImageUrl(),
                command.active());
        set.replaceRules(rules);
        return requirementSetRepository.save(set).getId();
    }

    /**
     * 졸업 요건 세트의 정보를 전체 교체하고, 연결 규칙도 통째로 교체한다.
     * 학과·단과대·버전은 생성 시 확정되어 수정 대상이 아니며 요청 자체에 포함되지 않는다. 적용년도가 바뀌면 버전을 새 lineage 기준으로 다시 부여하고, 그대로면 기존 버전을 유지한다.
     * 활성으로 둘 경우 적용년도가 겹치는 다른 활성 세트(자신 제외)가 없어야 한다.
     */
    @Transactional
    public void update(Long id, RequirementSetUpdateCommand command) {
        RequirementSet set = requirementSetRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));

        Long departmentId = set.getDepartmentId();
        validateActiveNotOverlapping(departmentId, command.yearStart(), command.yearEnd(), command.active(), id);
        int version = resolveUpdatedVersion(set, command.yearStart(), command.yearEnd());
        List<GraduationRule> rules = loadRules(command.graduationRuleIds());

        set.update(
                departmentId,
                command.yearStart(),
                command.yearEnd(),
                version,
                command.description(),
                command.sheetImageUrl(),
                command.active());
        set.replaceRules(rules);
    }

    /** (학과, 적용년도) lineage의 다음 버전을 반환한다. 기존 세트가 없으면 첫 버전 1. */
    private int nextVersion(Long departmentId, int yearStart, int yearEnd) {
        return requirementSetRepository
                .findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(departmentId, yearStart, yearEnd)
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);
    }

    /** 수정 시 적용년도가 그대로면 기존 버전을 유지하고, 바뀌면 옮겨갈 lineage의 다음 버전을 부여한다. */
    private int resolveUpdatedVersion(RequirementSet set, int yearStart, int yearEnd) {
        if (set.getYearStart() == yearStart && set.getYearEnd() == yearEnd) {
            return set.getVersion();
        }
        return nextVersion(set.getDepartmentId(), yearStart, yearEnd);
    }

    /**
     * 활성으로 저장하려는 경우, 같은 학과의 다른 활성 세트(자신 제외)와 적용년도 범위가 겹치면 예외를 던진다.
     * 비활성 저장은 검증하지 않는다(비활성 세트는 적용년도가 겹쳐도 공존 가능). selfId가 null이면 생성, non-null이면 수정.
     */
    private void validateActiveNotOverlapping(
            Long departmentId, int yearStart, int yearEnd, boolean active, Long selfId) {
        if (!active) {
            return;
        }
        // 메모리 로드 없이 DB exists로 겹침 판별. 파생 쿼리 인자 순서는 (departmentId, yearEnd, yearStart).
        boolean overlaps = (selfId == null)
                ? requirementSetRepository
                        .existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                                departmentId, yearEnd, yearStart)
                : requirementSetRepository
                        .existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqualAndIdNot(
                                departmentId, yearEnd, yearStart, selfId);
        if (overlaps) {
            throw new GeneralException(CurriculumErrorCode.ACTIVE_REQUIREMENT_SET_OVERLAP);
        }
    }

    /**
     * 입력받은 단과대명으로 기존 단과대를 찾으면 재사용하고, 없으면 새로 등록한다(find-or-create). 단과대명은 유일 키다.
     * 앞뒤 공백으로 인한 중복 생성을 막기 위해 trim한 값으로 조회·저장한다.
     */
    private Long resolveOrCreateCollege(String collegeName) {
        String name = collegeName.trim();
        return collegeRepository.findByCollegeName(name).map(College::getId).orElseGet(() -> collegeRepository
                .save(College.create(name))
                .getId());
    }

    /**
     * 입력받은 학과명으로 기존 학과를 찾으면 재사용하고, 없으면 새 학과를 등록한다(find-or-create). 학과명은 유일 키다.
     * 기존 학과가 입력 단과대와 다른 단과대 소속이면 정합성 보호를 위해 예외를 던진다(소속 단과대는 임의 변경하지 않는다).
     * 앞뒤 공백으로 인한 중복 생성을 막기 위해 trim한 값으로 조회·저장한다.
     */
    private Long resolveOrCreateDepartment(String departmentName, Long collegeId) {
        String name = departmentName.trim();
        return departmentRepository
                .findByDepartmentName(name)
                .map(existing -> {
                    if (!existing.getCollegeId().equals(collegeId)) {
                        throw new GeneralException(CurriculumErrorCode.DEPARTMENT_COLLEGE_MISMATCH);
                    }
                    return existing.getId();
                })
                .orElseGet(() -> departmentRepository
                        .save(Department.create(collegeId, name))
                        .getId());
    }

    private List<GraduationRule> loadRules(List<Long> graduationRuleIds) {
        if (graduationRuleIds == null || graduationRuleIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds =
                graduationRuleIds.stream().filter(Objects::nonNull).distinct().toList();
        List<GraduationRule> found = graduationRuleRepository.findAllById(distinctIds);
        if (found.size() != distinctIds.size()) {
            throw new GeneralException(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND);
        }
        return found;
    }
}
