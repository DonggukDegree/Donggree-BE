package com.donggree.curriculum.internal.application.projection;

import java.util.List;

/**
 * 관리자 대시보드용 졸업 요건 세트 조회 응답.
 * 연결된 졸업 규칙은 ID 목록으로 내려주며, 프론트는 졸업 규칙 조회 결과와 교차참조한다.
 */
public record RequirementSetProjection(
        Long id,
        Long departmentId,
        String departmentName,
        int yearStart,
        int yearEnd,
        int version,
        String description,
        String sheetImageUrl,
        boolean active,
        List<Long> graduationRuleIds) {}
