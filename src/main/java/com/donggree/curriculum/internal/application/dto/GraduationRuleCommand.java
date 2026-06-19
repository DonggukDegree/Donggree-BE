package com.donggree.curriculum.internal.application.dto;

/**
 * 졸업 규칙 등록/수정 입력 커맨드. 컨트롤러의 요청 DTO를 응용 계층 입력으로 변환한 값이다.
 * ruleConfig는 JSON 문자열(jsonb 저장값)이다.
 */
public record GraduationRuleCommand(Long ruleTypeId, String ruleName, String ruleConfig, String description) {}
