package com.donggree.graduation.internal.application.projection;

import java.math.BigDecimal;
import java.util.List;

/**
 * 학업 리포트 조회 결과 프로젝션.
 * 애그리거트(엔티티)가 없는 무상태 판정 모듈이므로, 여러 모듈의 조회 결과를 응용 계층에서
 * 조립한 읽기 전용 모델이다. 컨트롤러가 그대로 HTTP 응답으로 반환한다.
 * summary: 졸업 달성률 요약 / areaOverviews: courseType별 이수 현황
 * hasUnsupportedMajor: 판정에 반영되지 않은 전공(복수전공·부전공) 이력 유무.
 */
public record GraduationReportProjection(
        Summary summary, List<AreaOverview> areaOverviews, boolean hasUnsupportedMajor) {

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
     * achievementRate: 해당 courseType 규칙의 충족 비율 (충족 규칙 수 / 전체 규칙 수 × 100)
     * remainingCredits: max(0, targetCredits - earnedCredits) — targetCredits는 MIN_CREDITS 규칙에서 결정
     * satisfied: 해당 courseType에 적용되는 모든 규칙 충족 여부
     */
    public record AreaOverview(
            String courseType, String courseTypeName, int achievementRate, int remainingCredits, boolean satisfied) {}
}
