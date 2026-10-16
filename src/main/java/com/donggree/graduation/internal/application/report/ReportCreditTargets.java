package com.donggree.graduation.internal.application.report;

import static com.donggree.graduation.internal.application.report.ReportRuleConfig.hasNumberField;
import static com.donggree.graduation.internal.application.report.ReportRuleConfig.parseIntField;
import static com.donggree.graduation.internal.application.report.ReportRuleConfig.parseStringList;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import com.donggree.graduation.internal.domain.CreditAdjustmentCalculator;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/** 리포트에 표시할 이수구분별 목표·잔여 학점 계산. */
final class ReportCreditTargets {
    private static final String MIN_CREDITS = "MIN_CREDITS";

    private ReportCreditTargets() {}

    static AreaDetailReadModel.CreditStatus buildTypeCredits(
            CourseType courseType, List<GraduationRuleView> areaRules, EvaluationContext context) {
        int earned = context.getTotalPassedCreditsByType(courseType);
        int target = resolveTypeTargetCredits(courseType, areaRules, context);
        return new AreaDetailReadModel.CreditStatus(earned, target, Math.max(0, target - earned));
    }

    static int resolveTypeTargetCredits(
            CourseType courseType, List<GraduationRuleView> rules, EvaluationContext context) {
        if (courseType == CourseType.ACADEMIC_FOUNDATION) {
            return AcademicFoundationCreditTargets.total(rules, context);
        }
        OptionalInt wholeType = rules.stream()
                .filter(ReportCreditTargets::isCreditTargetRule)
                .filter(r -> parseStringList(r.ruleConfig(), "areaNames").isEmpty())
                .mapToInt(r -> CreditAdjustmentCalculator.adjustedTarget(
                        r, context, parseIntField(r.ruleConfig(), "minCredits", 0)))
                .max();
        if (wholeType.isPresent()) return wholeType.getAsInt();

        // A/B 학과가 같은 영역 학점을 요구해도 학생이 그 학점을 두 번 취득할 필요는 없다.
        // 동일 영역 묶음은 더 큰 기준을 표시하고, 서로 다른 묶음은 기존처럼 합산한다.
        return rules.stream()
                .filter(ReportCreditTargets::isCreditTargetRule)
                .filter(r -> !parseStringList(r.ruleConfig(), "areaNames").isEmpty())
                .collect(Collectors.toMap(
                        r -> parseStringList(r.ruleConfig(), "areaNames").stream()
                                .distinct()
                                .sorted()
                                .toList(),
                        r -> CreditAdjustmentCalculator.adjustedTarget(
                                r, context, parseIntField(r.ruleConfig(), "minCredits", 0)),
                        Math::max))
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    static boolean isCreditTargetRule(GraduationRuleView rule) {
        return MIN_CREDITS.equals(rule.typeName()) && hasNumberField(rule.ruleConfig(), "minCredits");
    }

    static String courseTypeKoreanName(CourseType courseType) {
        return switch (courseType) {
            case COMMON_GENERAL -> "공통교양";
            case ACADEMIC_FOUNDATION -> "학문기초";
            case LIBERAL_ARTS -> "일반교양";
            case FIRST_MAJOR -> "제1전공";
            case SECOND_MAJOR -> "제2전공";
        };
    }
}
