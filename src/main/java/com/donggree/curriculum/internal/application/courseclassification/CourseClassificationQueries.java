package com.donggree.curriculum.internal.application.courseclassification;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.readmodel.CourseClassificationReadModel;
import java.util.List;

/**
 * 과목 분류와 영역명을 반환하는 응용 계층 조회 계약.
 */
public interface CourseClassificationQueries {

    /**
     * 동적 다중 필터로 과목 분류를 조회한다. 각 목록이 null이거나 비어 있으면 해당 조건은 무시한다(미지정=전체).
     * 등록된 영역이 없으면 areaName은 null이다.
     * - areaTypeIds: 이수 영역 ID 중 하나라도 일치(IN)
     * - courseTypes: 이수구분 중 하나라도 일치(IN)
     * - years: 지정한 연도 중 하나라도 적용 입학년도 범위(start≤year≤end)에 포함되면 일치(OR)
     */
    List<CourseClassificationReadModel> search(
            List<Long> areaTypeIds, List<CourseType> courseTypes, List<Integer> years);
}
