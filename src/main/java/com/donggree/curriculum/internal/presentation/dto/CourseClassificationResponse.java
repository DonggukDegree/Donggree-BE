package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.CourseClassificationProjection;

/**
 * 과목 분류 조회 응답. area_type_id 대신 영역 이름(areaName)을 함께 내려주며, 편집 왕복을 위해 areaTypeId도 포함한다.
 */
public record CourseClassificationResponse(
        Long id,
        String courseCode,
        String tag,
        int studentYearStart,
        int studentYearEnd,
        CourseType courseType,
        Long areaTypeId,
        String areaName,
        String subCategory,
        String subjectDomain) {
    public static CourseClassificationResponse from(CourseClassificationProjection p) {
        return new CourseClassificationResponse(
                p.id(),
                p.courseCode(),
                p.tag(),
                p.studentYearStart(),
                p.studentYearEnd(),
                p.courseType(),
                p.areaTypeId(),
                p.areaName(),
                p.subCategory(),
                p.subjectDomain());
    }
}
