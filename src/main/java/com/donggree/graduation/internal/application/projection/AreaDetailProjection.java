package com.donggree.graduation.internal.application.projection;

import java.util.List;

/**
 * 영역별 이수 현황 조회 결과 프로젝션.
 * 애그리거트를 거치지 않고 여러 모듈 조회 결과를 응용 계층에서 조립한 읽기 전용 모델이다.
 * areaDetails: courseType 내 areaName별 이수 현황
 * unsatisfiedReasons: 해당 courseType에서 미충족된 rule_name 목록
 * creditStatus: 해당 courseType 전체 학점 이수 현황
 */
public record AreaDetailProjection(
        List<AreaSection> areaDetails, List<String> unsatisfiedReasons, CreditStatus creditStatus) {

    /**
     * targetCredits는 단일 영역 규칙에서만 채워진다.
     * 여러 영역을 합산하는 규칙(ex. MSC 수학+과학 21학점)의 목표학점은 어느 한 영역에 귀속시킬 수 없으므로
     * 해당 영역 섹션의 targetCredits는 0이다. 그 요건은 unsatisfiedReasons에 규칙명으로 노출된다.
     */
    public record AreaSection(
            String areaName, int earnedCredits, int targetCredits, boolean satisfied, List<CourseItem> items) {}

    /**
     * status: SATISFIED(필수 이수), UNSATISFIED(필수 미이수), OPTIONAL(선택 이수)
     * detail: title이 alias(소분류명)인 경우 실제 수강 과목명 목록, 단일 과목이면 null
     */
    public record CourseItem(String title, int credit, String status, List<String> detail) {}

    public record CreditStatus(int earnedCredits, int targetCredits, int remainingCredits) {}
}
