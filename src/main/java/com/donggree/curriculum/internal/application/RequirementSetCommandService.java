package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.command.RequirementSetCommand;
import com.donggree.curriculum.internal.application.command.RequirementSetUpdateCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.college.College;
import com.donggree.curriculum.internal.domain.college.CollegeRepository;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
     * 버전은 (학과, 적용년도, 과정) lineage의 최신 버전+1로 자동 채번하며, 활성으로 등록할 경우
     * 적용년도와 적용 대상 학생이 함께 겹치는 기존 활성 세트가 없어야 한다.
     */
    @Transactional
    public Long create(RequirementSetCommand command) {
        Long collegeId = resolveOrCreateCollege(command.collegeName());
        Long departmentId = resolveOrCreateDepartment(command.departmentName(), collegeId);
        validateActiveNotOverlapping(
                departmentId, command.yearStart(), command.yearEnd(), command.track(), command.active(), null);
        int version = nextVersion(departmentId, command.yearStart(), command.yearEnd(), command.track());
        List<GraduationRule> rules = loadRules(command.graduationRuleIds());

        RequirementSet set = RequirementSet.create(
                departmentId,
                command.yearStart(),
                command.yearEnd(),
                command.track(),
                version,
                command.description(),
                command.sheetImageUrl(),
                command.active());
        set.replaceRules(rules);
        return requirementSetRepository.save(set).getId();
    }

    /**
     * 졸업 요건 세트의 정보를 전체 교체하고, 연결 규칙도 통째로 교체한다.
     * 학과·단과대·버전은 생성 시 확정되어 수정 대상이 아니며 요청 자체에 포함되지 않는다. 적용년도나 과정이 바뀌면 버전을 새 lineage 기준으로 다시 부여하고, 그대로면 기존 버전을 유지한다.
     * 활성으로 둘 경우 적용년도와 적용 대상 학생이 함께 겹치는 다른 활성 세트(자신 제외)가 없어야 한다.
     */
    @Transactional
    public void update(Long id, RequirementSetUpdateCommand command) {
        RequirementSet set = requirementSetRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));

        Long departmentId = set.getDepartmentId();
        validateActiveNotOverlapping(
                departmentId, command.yearStart(), command.yearEnd(), command.track(), command.active(), id);
        int version = resolveUpdatedVersion(set, command.yearStart(), command.yearEnd(), command.track());
        List<GraduationRule> rules = loadRules(command.graduationRuleIds());

        set.update(
                departmentId,
                command.yearStart(),
                command.yearEnd(),
                command.track(),
                version,
                command.description(),
                command.sheetImageUrl(),
                command.active());
        set.replaceRules(rules);
    }

    /** (학과, 적용년도, 과정) lineage의 다음 버전을 반환한다. 기존 세트가 없으면 첫 버전 1. */
    private int nextVersion(Long departmentId, int yearStart, int yearEnd, RequirementTrack track) {
        return requirementSetRepository
                .findTopByDepartmentIdAndYearStartAndYearEndAndTrackOrderByVersionDesc(
                        departmentId, yearStart, yearEnd, track)
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);
    }

    /** 수정 시 적용년도·과정이 그대로면 기존 버전을 유지하고, 바뀌면 옮겨갈 lineage의 다음 버전을 부여한다. */
    private int resolveUpdatedVersion(RequirementSet set, int yearStart, int yearEnd, RequirementTrack track) {
        if (set.getYearStart() == yearStart && set.getYearEnd() == yearEnd && set.getTrack() == track) {
            return set.getVersion();
        }
        return nextVersion(set.getDepartmentId(), yearStart, yearEnd, track);
    }

    /**
     * 활성으로 저장하려는 경우, 같은 학과의 다른 활성 세트(자신 제외)와 적용년도 범위가 겹치고
     * 적용 대상 학생까지 겹치면 예외를 던진다.
     *
     * <p>일반과정 세트와 심화과정 세트는 서로 다른 학생을 보므로 적용년도가 같아도 공존할 수 있다.
     * 반면 ALL 세트는 모든 학생을 받아 어느 과정과도 겹치므로, 같은 적용년도에 ALL과 GENERAL을
     * 함께 두면 한 학생에게 두 세트가 걸린다. 그래서 {@link RequirementTrack#conflictingTracks()}로
     * 겹칠 수 있는 과정만 추려 검사한다.
     *
     * <p>비활성 저장은 검증하지 않는다(비활성 세트는 적용년도가 겹쳐도 공존 가능).
     * selfId가 null이면 생성, non-null이면 수정.
     */
    private void validateActiveNotOverlapping(
            Long departmentId, int yearStart, int yearEnd, RequirementTrack track, boolean active, Long selfId) {
        if (!active) {
            return;
        }
        // 메모리 로드 없이 DB exists로 겹침 판별. 파생 쿼리 인자 순서는 (departmentId, tracks, yearEnd, yearStart).
        Set<RequirementTrack> conflicting = track.conflictingTracks();
        boolean overlaps = (selfId == null)
                ? requirementSetRepository
                        .existsByActiveTrueAndDepartmentIdAndTrackInAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                                departmentId, conflicting, yearEnd, yearStart)
                : requirementSetRepository
                        .existsByActiveTrueAndDepartmentIdAndTrackInAndYearStartLessThanEqualAndYearEndGreaterThanEqualAndIdNot(
                                departmentId, conflicting, yearEnd, yearStart, selfId);
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
     * 같은 단과대·학과명은 재사용하고, 다른 단과대면 별도 학과를 등록한다.
     * 학과 이동 전후의 졸업세트가 공존하도록 기존 학과의 소속·ID는 변경하지 않는다.
     * 앞뒤 공백으로 인한 중복 생성을 막기 위해 trim한 값으로 조회·저장한다.
     */
    private Long resolveOrCreateDepartment(String departmentName, Long collegeId) {
        String name = departmentName.trim();
        return departmentRepository
                .findByCollegeIdAndDepartmentName(collegeId, name)
                .map(Department::getId)
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
