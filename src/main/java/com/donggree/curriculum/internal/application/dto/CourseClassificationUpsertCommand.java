package com.donggree.curriculum.internal.application.dto;

/**
 * 과목 분류 배치 업서트의 단일 항목 커맨드.
 * id가 null이면 신규 등록, non-null이면 해당 분류를 전체 교체(수정)한다.
 */
public record CourseClassificationUpsertCommand(Long id, CourseClassificationCommand data) {}
