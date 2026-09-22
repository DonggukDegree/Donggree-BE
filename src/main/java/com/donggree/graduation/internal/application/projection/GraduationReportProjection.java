package com.donggree.graduation.internal.application.projection;

import java.math.BigDecimal;
import java.util.List;

/**
 * 학업 리포트 조회 결과 프로젝션.
 * 애그리거트(엔티티)가 없는 무상태 판정 모듈이므로, 여러 모듈의 조회 결과를 응용 계층에서
 * 조립한 읽기 전용 모델이다. 컨트롤러가 그대로 HTTP 응답으로 반환한다.
 * summary: 졸업 달성률 요약 / areaOverviews: courseType별 이수 현황
 * hasUnsupportedMajor: 복수전공 판정 누락·복수전공 2·부전공·편입 등으로 정확도 경고가 필요한지 여부.
 */
public record GraduationReportProjection(
        Summary summary, List<AreaOverview> areaOverviews, boolean hasUnsupportedMajor, Boolean englishPassed) {

    /**
     * 졸업 요건 전체 요약.
     * unsatisfiedReasons: 주전공 요건 중 이수구분이 없는 총학점·평점·영어강의·졸업시험 등의 미충족 사유
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
     * achievementRate: 제2전공은 복수전공 추가 요건, 그 외는 원래 이수구분 요건의 충족 비율
     * remainingCredits: max(0, targetCredits - earnedCredits) — targetCredits는 MIN_CREDITS 규칙에서 결정
     * satisfied: achievementRate와 같은 규칙 목록의 전체 충족 여부. 학점 집계는 원래 이수구분 유지
     */
    public record AreaOverview(
            String courseType, String courseTypeName, int achievementRate, int remainingCredits, boolean satisfied) {}
}
