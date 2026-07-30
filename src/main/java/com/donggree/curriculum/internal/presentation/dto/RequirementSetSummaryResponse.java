package com.donggree.curriculum.internal.presentation.dto;

import com.donggree.curriculum.internal.application.projection.RequirementSetSummaryProjection;

/**
 * 졸업 요건 세트 목록용 요약 응답. 연결 규칙 목록은 포함하지 않는다(상세 조회에서 제공).
 */
public record RequirementSetSummaryResponse(
        Long id,
        Long departmentId,
        String departmentName,
        int yearStart,
        int yearEnd,
        int version,
        String description,
        String sheetImageUrl,
        boolean active) {
    public static RequirementSetSummaryResponse from(RequirementSetSummaryProjection p) {
        return new RequirementSetSummaryResponse(
                p.id(),
                p.departmentId(),
                p.departmentName(),
                p.yearStart(),
                p.yearEnd(),
                p.version(),
                p.description(),
                p.sheetImageUrl(),
                p.active());
    }
}
