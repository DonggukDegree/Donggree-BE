package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.RuleTypeProjection;

/** 규칙 종류 조회 응답. */
public record RuleTypeResponse(Long id, String typeName, CourseType courseType, String description) {
    public static RuleTypeResponse from(RuleTypeProjection p) {
        return new RuleTypeResponse(p.id(), p.typeName(), p.courseType(), p.description());
    }
}
