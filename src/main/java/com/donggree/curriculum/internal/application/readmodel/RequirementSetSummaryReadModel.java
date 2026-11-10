package com.donggree.curriculum.internal.application.readmodel;

import com.donggree.curriculum.internal.domain.enums.RequirementTrack;

/**
 * 졸업 요건 세트 목록용 요약 읽기 모델. 연결 규칙 목록은 포함하지 않는다(상세 조회에서 제공).
 */
public record RequirementSetSummaryReadModel(
        Long id,
        Long departmentId,
        String departmentName,
        int yearStart,
        int yearEnd,
        RequirementTrack track,
        int version,
        String description,
        String sheetImageUrl,
        boolean active) {}
