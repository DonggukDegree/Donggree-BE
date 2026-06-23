package com.donggree.curriculum.internal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementSetRepository extends JpaRepository<RequirementSet, Long>, RequirementSetRepositoryCustom {

    List<RequirementSet> findByDepartmentIdAndActiveTrue(Long departmentId);

    /**
     * 같은 (학과, 적용년도 범위) lineage의 최신 버전 세트를 조회한다. 생성/수정 시 다음 버전 자동 채번에 사용한다.
     * 없으면 첫 버전(1)을 부여한다.
     */
    Optional<RequirementSet> findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(
            Long departmentId, int yearStart, int yearEnd);
}
