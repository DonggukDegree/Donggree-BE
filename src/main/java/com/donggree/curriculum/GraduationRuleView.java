package com.donggree.curriculum;

/**
 * 다른 모듈에 졸업 규칙 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * typeName은 graduation 모듈의 RuleEvaluator 분기 식별자로 사용한다 (ex. TOTAL_CREDITS, REQUIRED_COURSE).
 * ruleConfig는 JSON 문자열이며 typeName별 스키마가 다르다.
 */
public record GraduationRuleView(Long id, String typeName, RuleCategory category, String ruleName, String ruleConfig) {}
