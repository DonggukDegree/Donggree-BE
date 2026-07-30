package com.donggree.curriculum.internal.domain.requirementset;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementSetRepository extends JpaRepository<RequirementSet, Long> {

    List<RequirementSet> findByDepartmentIdAndActiveTrue(Long departmentId);

    /**
     * 같은 (학과, 적용년도 범위) lineage의 최신 버전 세트를 조회한다. 생성/수정 시 다음 버전 자동 채번에 사용한다.
     * 없으면 첫 버전(1)을 부여한다.
     */
    Optional<RequirementSet> findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(
            Long departmentId, int yearStart, int yearEnd);

    /**
     * 같은 학과에 적용년도 범위가 겹치는 활성 세트가 있는지 DB에서 직접 판별한다(생성 검증용).
     * 범위 겹침 조건(yearStart ≤ 신규 yearEnd AND yearEnd ≥ 신규 yearStart)을 파생 쿼리로 표현했다.
     */
    boolean existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
            Long departmentId, int yearEnd, int yearStart);

    /** 위와 동일하되 자기 자신(id)은 제외한다(수정 검증용). */
    boolean existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqualAndIdNot(
            Long departmentId, int yearEnd, int yearStart, Long id);
}
