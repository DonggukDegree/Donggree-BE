package com.donggree.graduation.internal.application.report;

import static com.donggree.graduation.internal.application.report.ReportCreditTargets.courseTypeKoreanName;
import static com.donggree.graduation.internal.application.report.ReportCreditTargets.resolveTypeTargetCredits;
import static com.donggree.graduation.internal.application.report.ReportRuleConfig.parseIntField;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection.AreaOverview;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection.Summary;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.TranscriptView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 전체 졸업 요약과 이수구분별 개요 조립. */
final class GraduationSummaryAssembler {
    private static final String MIN_CREDITS = "MIN_CREDITS";

    private GraduationSummaryAssembler() {}

    static Summary buildSummary(
            TranscriptView transcript,
            List<GraduationRuleView> rules,
            List<GraduationRuleView> statusRules,
            Map<Long, RuleResult> resultByRuleId) {
        long satisfiedCount =
                resultByRuleId.values().stream().filter(RuleResult::satisfied).count();
        int achievementRate = rules.isEmpty() ? 0 : (int) (satisfiedCount * 100 / rules.size());

        int targetCredits = rules.stream()
                .filter(r -> "TOTAL_CREDITS".equals(r.typeName()))
                .findFirst()
                .map(r -> parseIntField(r.ruleConfig(), "minCredits", 130))
                .orElse(130);

        int earnedCredits = transcript.totalCredits();
        int remainingCredits = Math.max(0, targetCredits - earnedCredits);

        boolean graduated =
                !resultByRuleId.isEmpty() && resultByRuleId.values().stream().allMatch(RuleResult::satisfied);

        // 학과에 관계없이 이수구분 없는 일반 요건은 요약에 표시한다.
        List<String> unsatisfiedReasons = statusRules.stream()
                .filter(r -> r.courseType() == null)
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && !result.satisfied();
                })
                .map(GraduationRuleView::ruleName)
                // 둘 중 하나라도 미충족이면 사유를 남기되, 동일한 문구는 한 번만 표시한다.
                .distinct()
                .toList();

        return new Summary(
                achievementRate,
                earnedCredits,
                targetCredits,
                remainingCredits,
                transcript.gpa(),
                graduated,
                unsatisfiedReasons);
    }

    static List<AreaOverview> buildAreaOverviews(
            List<GraduationRuleView> rules,
            List<GraduationRuleView> statusRules,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context) {
        Map<CourseType, List<GraduationRuleView>> rulesByCourseType = rules.stream()
                .filter(r -> r.courseType() != null)
                .collect(Collectors.groupingBy(GraduationRuleView::courseType));

        Map<CourseType, List<GraduationRuleView>> statusRulesByCourseType = statusRules.stream()
                .filter(r -> r.courseType() != null)
                .collect(Collectors.groupingBy(GraduationRuleView::courseType));

        Map<CourseType, List<GraduationRuleView>> minAreaRulesByCourseType = rules.stream()
                .filter(r -> r.courseType() != null && MIN_CREDITS.equals(r.typeName()))
                .collect(Collectors.groupingBy(GraduationRuleView::courseType));

        List<CourseType> relevantCourseTypes = new ArrayList<>();
        for (CourseType ct : CourseType.values()) {
            if (rulesByCourseType.containsKey(ct)
                    || statusRulesByCourseType.containsKey(ct)
                    || context.getTotalPassedCreditsByType(ct) > 0) {
                relevantCourseTypes.add(ct);
            }
        }

        return relevantCourseTypes.stream()
                // 두 전공 카드 모두 해당 전공으로 분류된 요건만 반영한다.
                .map(ct -> buildAreaOverview(
                        ct,
                        ct == CourseType.FIRST_MAJOR || ct == CourseType.SECOND_MAJOR
                                ? statusRulesByCourseType
                                : rulesByCourseType,
                        minAreaRulesByCourseType,
                        resultByRuleId,
                        context))
                .toList();
    }

    static AreaOverview buildAreaOverview(
            CourseType courseType,
            Map<CourseType, List<GraduationRuleView>> rulesByCourseType,
            Map<CourseType, List<GraduationRuleView>> minAreaRulesByCourseType,
            Map<Long, RuleResult> resultByRuleId,
            EvaluationContext context) {

        List<GraduationRuleView> areaRules = rulesByCourseType.getOrDefault(courseType, List.of());
        List<GraduationRuleView> minAreaRules = minAreaRulesByCourseType.getOrDefault(courseType, List.of());

        long satisfiedCount = areaRules.stream()
                .filter(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && result.satisfied();
                })
                .count();
        int achievementRate = areaRules.isEmpty() ? 100 : (int) (satisfiedCount * 100 / areaRules.size());

        boolean satisfied = areaRules.isEmpty()
                || areaRules.stream().allMatch(r -> {
                    RuleResult result = resultByRuleId.get(r.id());
                    return result != null && result.satisfied();
                });

        int earnedCredits = context.getTotalPassedCreditsByType(courseType);
        int remainingCredits = Math.max(0, resolveTypeTargetCredits(courseType, minAreaRules, context) - earnedCredits);

        return new AreaOverview(
                courseType.name(), courseTypeKoreanName(courseType), achievementRate, remainingCredits, satisfied);
    }
}
