package com.donggree.curriculum.internal.application.dto;

/**
 * 졸업 규칙 배치 업서트의 단일 항목 커맨드.
 * id가 null이면 신규 등록, non-null이면 해당 규칙을 전체 교체(수정)한다.
 */
public record GraduationRuleUpsertCommand(Long id, GraduationRuleCommand data) {}
