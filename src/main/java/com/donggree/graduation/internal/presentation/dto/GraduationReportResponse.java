package com.donggree.graduation.internal.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/reports/{reportId} 응답 DTO.
 * summary: 졸업 달성률 요약 / areaOverviews: courseType별 이수 현황
 */
public record GraduationReportResponse(Summary summary, List<AreaOverview> areaOverviews) {

    /**
     * 졸업 요건 전체 요약.
     * unsatisfiedReasons: GRADUATION_REQ 카테고리에서 미충족된 rule_name 목록
     */
    public record Summary(
            int achievementRate,
            int earnedCredits,
            int targetCredits,
            int remainingCredits,
            BigDecimal gpa,
            boolean graduated,
            List<String> unsatisfiedReasons) {}

    /**
     * courseType별 이수 현황.
     * achievementRate: MIN_AREA_CREDITS 기준 학점 달성률 (earnedCredits / targetCredits × 100, max 100)
     * remainingCredits: max(0, targetCredits - earnedCredits)
     * satisfied: 해당 courseType에 적용되는 모든 MIN_AREA_CREDITS 규칙 충족 여부
     */
    public record AreaOverview(
            String courseType, String courseTypeName, int achievementRate, int remainingCredits, boolean satisfied) {}
}
