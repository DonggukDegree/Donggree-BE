package com.donggree.curriculum.internal.application.dto;

import com.donggree.curriculum.CourseType;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * 관리자 대시보드용 졸업 규칙 조회 응답.
 * rule_type 정보(typeName, courseType)를 함께 내려준다.
 * ruleConfig는 jsonb 원문을 그대로 JSON 객체로 직렬화한다(@JsonRawValue) — 문자열이 아니라 객체로 내려간다.
 */
public record GraduationRuleResponse(
        Long id,
        Long ruleTypeId,
        String typeName,
        CourseType courseType,
        String ruleName,
        @JsonRawValue String ruleConfig,
        String description) {}
