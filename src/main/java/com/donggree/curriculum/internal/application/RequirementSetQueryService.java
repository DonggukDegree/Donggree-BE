package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.application.projection.RequirementSetProjection;
import com.donggree.curriculum.internal.application.projection.RequirementSetSummaryProjection;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepositoryCustom;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 졸업 요건 세트(requirement_set) 조회(Query) 응용 서비스.
 * 상태를 변경하지 않는 읽기 유스케이스만 담당한다.
 * 목록(search)은 엔티티 로딩 없이 QueryDSL 프로젝션으로 조회하고, 단건(get)은 연결 규칙 컬렉션이
 * 필요하므로 애그리거트를 로딩해 조립한다.
 */
@Service
@RequiredArgsConstructor
public class RequirementSetQueryService {

    private final RequirementSetRepositoryCustom requirementSetQueryRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * 졸업 요건 세트 목록을 동적 필터로 조회한다(요약, 연결 규칙 미포함). department를 조인해 학과명을 함께 담는다.
     * departmentId(특정 학과)와 collegeId(단과대 소속 학과 전체)를 함께 걸 수 있고, year는 단일 연도로 적용범위(start≤year≤end)에 포함되는 세트를 반환한다.
     * 모든 조건이 null이면 무시(미지정=전체).
     */
    @Transactional(readOnly = true)
    public List<RequirementSetSummaryProjection> search(Long departmentId, Long collegeId, Integer year) {
        List<Long> departmentIds = resolveDepartmentIds(departmentId, collegeId);
        if (departmentIds != null && departmentIds.isEmpty()) {
            return List.of(); // 학과/단과대 필터는 있으나 해당하는 학과가 없음 → 빈 결과
        }
        return requirementSetQueryRepository.search(departmentIds, year);
    }

    /**
     * departmentId·collegeId 필터를 조회 대상 학과 ID 목록으로 해석한다.
     * 둘 다 없으면 null(학과 필터 없음). 둘 다 있으면 교집합(단과대에 속한 그 학과)만, 단과대에 그 학과가 없으면 빈 목록.
     */
    private List<Long> resolveDepartmentIds(Long departmentId, Long collegeId) {
        List<Long> collegeDepartmentIds = (collegeId == null)
                ? null
                : departmentRepository.findByCollegeId(collegeId).stream()
                        .map(Department::getId)
                        .toList();
        if (departmentId == null) {
            return collegeDepartmentIds;
        }
        if (collegeDepartmentIds == null) {
            return List.of(departmentId);
        }
        return collegeDepartmentIds.contains(departmentId) ? List.of(departmentId) : List.of();
    }

    /** 졸업 요건 세트 단건을 조회한다. 연결 규칙 ID 목록이 필요하므로 애그리거트를 로딩해 조립한다. */
    @Transactional(readOnly = true)
    public RequirementSetProjection get(Long id) {
        RequirementSet set = requirementSetRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));
        String departmentName = departmentRepository
                .findById(set.getDepartmentId())
                .map(Department::getDepartmentName)
                .orElse(null);
        List<Long> ruleIds = set.getRules().stream().map(GraduationRule::getId).toList();
        return new RequirementSetProjection(
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
