package com.donggree.graduation.internal.domain;

/**
 * 졸업 규칙 하나의 평가 결과를 나타내는 값 객체.
 * ruleName은 graduation_rule.rule_name 그대로이며, 미충족 시 사용자에게 노출된다.
 */
public record RuleResult(String ruleName, boolean satisfied) {}
