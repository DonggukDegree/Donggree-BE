package com.donggree.graduation.internal.presentation.dto;

import java.util.List;

/**
 * GET /api/reports/{reportId}?courseType={courseType} 응답 DTO.
 * areaDetails: courseType 내 areaName별 이수 현황
 * unsatisfiedReasons: 해당 courseType에서 미충족된 rule_name 목록
 * creditStatus: 해당 courseType 전체 학점 이수 현황
 */
public record AreaDetailResponse(
        List<AreaSection> areaDetails, List<String> unsatisfiedReasons, CreditStatus creditStatus) {

    public record AreaSection(
            String areaName, int earnedCredits, int targetCredits, boolean satisfied, List<CourseItem> items) {}

    /**
     * status: SATISFIED(필수 이수), UNSATISFIED(필수 미이수), OPTIONAL(선택 이수)
     * detail: title이 alias(소분류명)인 경우 실제 수강 과목명 목록, 단일 과목이면 null
     */
    public record CourseItem(String title, int credit, String status, List<String> detail) {}

    public record CreditStatus(int earnedCredits, int targetCredits, int remainingCredits) {}
}
