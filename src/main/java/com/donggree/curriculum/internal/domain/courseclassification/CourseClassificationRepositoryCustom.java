package com.donggree.curriculum.internal.domain.courseclassification;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.CourseClassificationProjection;
import java.util.List;

/**
 * 과목 분류 동적 조회를 위한 커스텀 리포지토리. QueryDSL로 구현한다.
 * 엔티티를 로딩하지 않고 area_type을 조인해 읽기 전용 프로젝션으로 바로 조회한다.
 */
public interface CourseClassificationRepositoryCustom {

    /**
     * 동적 다중 필터로 과목 분류를 조회한다. 각 목록이 null이거나 비어 있으면 해당 조건은 무시한다(미지정=전체).
     * area_type을 left join하여 areaName을 함께 채운다(미등록 영역이면 null).
     * - areaTypeIds: 이수 영역 ID 중 하나라도 일치(IN)
     * - courseTypes: 이수구분 중 하나라도 일치(IN)
     * - years: 지정한 연도 중 하나라도 적용 입학년도 범위(start≤year≤end)에 포함되면 일치(OR)
     */
    List<CourseClassificationProjection> search(
            List<Long> areaTypeIds, List<CourseType> courseTypes, List<Integer> years);
}
