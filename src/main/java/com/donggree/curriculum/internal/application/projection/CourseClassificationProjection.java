package com.donggree.curriculum.internal.application.projection;

import com.donggree.curriculum.CourseType;

/**
 * 관리자 대시보드용 과목 분류 조회 응답.
 * area_type_id 대신 영역 이름(areaName)을 함께 내려주며, 편집 왕복을 위해 areaTypeId도 포함한다.
 * tag는 과목명 등 표시용 자유 텍스트다.
 */
public record CourseClassificationProjection(
        Long id,
        String courseCode,
        String tag,
        int studentYearStart,
        int studentYearEnd,
        CourseType courseType,
        Long areaTypeId,
        String areaName,
        String subCategory,
        String subjectDomain) {}
