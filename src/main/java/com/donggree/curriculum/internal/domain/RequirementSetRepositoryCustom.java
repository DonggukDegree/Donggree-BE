package com.donggree.curriculum.internal.domain;

import java.util.List;

/**
 * 졸업 요건 세트 동적 조회를 위한 커스텀 리포지토리. QueryDSL로 구현한다.
 */
public interface RequirementSetRepositoryCustom {

    /**
     * 동적 필터로 졸업 요건 세트를 조회한다. 각 파라미터가 null이면 해당 조건은 무시한다(미지정=전체).
     * departmentIds는 단과대명으로 해석한 학과 ID 묶음으로, 그 학과들에 속한 세트만 반환한다(IN).
     * year는 단일 연도로, year_start ≤ year ≤ year_end 인 세트를 반환한다.
     */
    List<RequirementSet> search(List<Long> departmentIds, Integer year);
}
