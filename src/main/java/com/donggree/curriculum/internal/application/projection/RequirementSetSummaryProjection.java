package com.donggree.curriculum.internal.application.projection;

/**
 * 졸업 요건 세트 목록용 요약 응답. 연결 규칙 목록은 포함하지 않는다(상세 조회에서 제공).
 */
public record RequirementSetSummaryProjection(
        Long id,
        Long departmentId,
        String departmentName,
        int yearStart,
        int yearEnd,
        int version,
        String description,
        String sheetImageUrl,
        boolean active) {}
