package com.donggree.curriculum.internal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementSetRepository extends JpaRepository<RequirementSet, Long>, RequirementSetRepositoryCustom {

    List<RequirementSet> findByDepartmentIdAndActiveTrue(Long departmentId);

    /** 학과 + 적용년도 범위 + 버전으로 조회한다. 등록/수정 시 unique 제약 충돌 사전 검증에 사용한다. */
    Optional<RequirementSet> findByDepartmentIdAndYearStartAndYearEndAndVersion(
            Long departmentId, int yearStart, int yearEnd, int version);
}
