package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.dto.RequirementSetCommand;
import com.donggree.curriculum.internal.application.dto.RequirementSetResponse;
import com.donggree.curriculum.internal.application.dto.RequirementSetSummaryResponse;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.Department;
import com.donggree.curriculum.internal.domain.DepartmentRepository;
import com.donggree.curriculum.internal.domain.GraduationRule;
import com.donggree.curriculum.internal.domain.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.RequirementSet;
import com.donggree.curriculum.internal.domain.RequirementSetRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 졸업 요건 세트(requirement_set) 관리를 담당하는 응용 서비스.
 * 세트 1건을 단위로 생성·수정하며, 연결 졸업 규칙은 선택/해제 결과 전체를 받아 통째로 교체한다.
 */
@Service
@RequiredArgsConstructor
public class RequirementSetAdminService {

    private final RequirementSetRepository requirementSetRepository;
    private final GraduationRuleRepository graduationRuleRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * 졸업 요건 세트 목록을 동적 필터로 조회한다(요약, 연결 규칙 미포함).
     * departmentId·year가 null이면 해당 조건 무시. year는 단일 연도로 적용범위(start≤year≤end)에 포함되는 세트를 반환한다.
     */
    @Transactional(readOnly = true)
    public List<RequirementSetSummaryResponse> search(Long departmentId, Integer year) {
        List<RequirementSet> sets = requirementSetRepository.search(departmentId, year);
        Map<Long, String> departmentNames = loadDepartmentNames(sets);
        return sets.stream()
                .map(set -> toSummary(set, departmentNames.get(set.getDepartmentId())))
                .toList();
    }

    /** 졸업 요건 세트 단건을 조회한다. */
    @Transactional(readOnly = true)
    public RequirementSetResponse get(Long id) {
        RequirementSet set = requirementSetRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));
        String departmentName = departmentRepository
                .findById(set.getDepartmentId())
                .map(Department::getDepartmentName)
                .orElse(null);
        return toResponse(set, departmentName);
    }

    /** 새 졸업 요건 세트를 생성하고 선택한 졸업 규칙들을 연결한다. */
    @Transactional
    public Long create(RequirementSetCommand command) {
        validateDepartmentExists(command.departmentId());
        validateNotDuplicate(command, null);
        List<GraduationRule> rules = loadRules(command.graduationRuleIds());

        RequirementSet set = RequirementSet.create(
                command.departmentId(),
                command.yearStart(),
                command.yearEnd(),
                command.version(),
                command.description(),
                command.sheetImageUrl(),
                command.active());
        set.replaceRules(rules);
        return requirementSetRepository.save(set).getId();
    }

    /** 졸업 요건 세트의 정보를 전체 교체하고, 연결 규칙도 통째로 교체한다. */
    @Transactional
    public void update(Long id, RequirementSetCommand command) {
        RequirementSet set = requirementSetRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));

        validateDepartmentExists(command.departmentId());
        validateNotDuplicate(command, id);
        List<GraduationRule> rules = loadRules(command.graduationRuleIds());

        set.update(
                command.departmentId(),
                command.yearStart(),
                command.yearEnd(),
                command.version(),
                command.description(),
                command.sheetImageUrl(),
                command.active());
        set.replaceRules(rules);
    }

    private void validateDepartmentExists(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new GeneralException(CurriculumErrorCode.DEPARTMENT_NOT_FOUND);
        }
    }

    private void validateNotDuplicate(RequirementSetCommand command, Long selfId) {
        requirementSetRepository
                .findByDepartmentIdAndYearStartAndYearEndAndVersion(
                        command.departmentId(), command.yearStart(), command.yearEnd(), command.version())
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> {
                    throw new GeneralException(CurriculumErrorCode.DUPLICATE_REQUIREMENT_SET);
                });
    }

    private List<GraduationRule> loadRules(List<Long> graduationRuleIds) {
        if (graduationRuleIds == null || graduationRuleIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = graduationRuleIds.stream().distinct().toList();
        List<GraduationRule> found = graduationRuleRepository.findAllById(distinctIds);
        if (found.size() != distinctIds.size()) {
            throw new GeneralException(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND);
        }
        return found;
    }

    private Map<Long, String> loadDepartmentNames(List<RequirementSet> sets) {
        List<Long> departmentIds =
                sets.stream().map(RequirementSet::getDepartmentId).distinct().toList();
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));
    }

    private RequirementSetSummaryResponse toSummary(RequirementSet set, String departmentName) {
        return new RequirementSetSummaryResponse(
                set.getId(),
                set.getDepartmentId(),
                departmentName,
                set.getYearStart(),
                set.getYearEnd(),
                set.getVersion(),
                set.getDescription(),
                set.getSheetImageUrl(),
                set.isActive());
    }

    private RequirementSetResponse toResponse(RequirementSet set, String departmentName) {
        List<Long> ruleIds = set.getRules().stream().map(GraduationRule::getId).toList();
        return new RequirementSetResponse(
                set.getId(),
                set.getDepartmentId(),
                departmentName,
                set.getYearStart(),
                set.getYearEnd(),
                set.getVersion(),
                set.getDescription(),
                set.getSheetImageUrl(),
                set.isActive(),
                ruleIds);
    }
}
