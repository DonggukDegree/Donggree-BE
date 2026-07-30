package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.GraduationRuleProjection;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 졸업 규칙 조회 응답. ruleConfig는 jsonb 원문을 그대로 JSON 객체로 직렬화한다(@JsonRawValue).
 */
public record GraduationRuleResponse(
        Long id,
        Long ruleTypeId,
        String typeName,
        CourseType courseType,
        String ruleName,
        @JsonRawValue String ruleConfig,
        String description) {
    public static GraduationRuleResponse from(GraduationRuleProjection p) {
        return new GraduationRuleResponse(
                p.id(), p.ruleTypeId(), p.typeName(), p.courseType(), p.ruleName(), p.ruleConfig(), p.description());
    }
}
