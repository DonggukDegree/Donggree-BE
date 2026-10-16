package com.donggree.curriculum.internal.application.requirementset;

import com.donggree.curriculum.internal.application.readmodel.RequirementSetSummaryReadModel;
import java.util.List;

/**
 * 학과명과 적용 범위를 포함한 졸업 요건 세트 요약 조회 계약. 연결 규칙은 포함하지 않는다.
 */
public interface RequirementSetQueries {

    /**
     * 동적 필터로 졸업 요건 세트를 조회한다. 각 파라미터가 null이면 해당 조건은 무시한다(미지정=전체).
     * departmentIds는 조회할 학과 ID 묶음으로, 그 학과들에 속한 세트만 반환한다(IN).
     * year는 단일 연도로, 시작 연도 ≤ year ≤ 종료 연도 인 세트를 반환한다.
     */
    List<RequirementSetSummaryReadModel> search(List<Long> departmentIds, Integer year);
}
