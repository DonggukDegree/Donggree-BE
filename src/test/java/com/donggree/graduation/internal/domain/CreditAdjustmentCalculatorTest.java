package com.donggree.graduation.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreditAdjustmentCalculatorTest {

    @Test
    void 대체과목_하나로_여러_규칙의_학점을_중복_감면하지_않는다() {
        EvaluationContext context = context(passed("ALT", true));
        GraduationRuleView rule = rule(adjustment("REQ1", "ALT", 3), adjustment("REQ2", "ALT", 3));

        assertThat(CreditAdjustmentCalculator.adjustedTarget(rule, context, 12)).isEqualTo(9);
    }

    @Test
    void 서로_다른_대체과목은_각각_한_규칙씩_대체한다() {
        EvaluationContext context = context(passed("ALT1", true), passed("ALT2", true));
        GraduationRuleView rule = rule(adjustment("REQ1", "ALT1", 3), adjustment("REQ2", "ALT2", 3));

        assertThat(CreditAdjustmentCalculator.adjustedTarget(rule, context, 12)).isEqualTo(6);
    }

    @Test
    void 대체과목_후보가_겹치면_총_감면학점이_최대가_되도록_배정한다() {
        EvaluationContext context = context(passed("ALT1", true), passed("ALT2", true));
        GraduationRuleView rule =
                rule(adjustment("REQ1", List.of("ALT1", "ALT2"), 3), adjustment("REQ2", List.of("ALT1"), 6));

        assertThat(CreditAdjustmentCalculator.adjustedTarget(rule, context, 20)).isEqualTo(11);
    }

    @Test
    void 이미_필수과목으로_이수한_과목은_다른_규칙의_대체과목으로_재사용하지_않는다() {
        EvaluationContext context = context(passed("REQ1", true));
        GraduationRuleView rule = rule(adjustment("REQ1", "ALT1", 3), adjustment("REQ2", "REQ1", 3));

        assertThat(CreditAdjustmentCalculator.adjustedTarget(rule, context, 12)).isEqualTo(12);
    }

    @Test
    void 미이수_대체과목은_목표학점을_감면하지_않는다() {
        EvaluationContext context = context(passed("ALT1", false));
        GraduationRuleView rule = rule(adjustment("REQ1", "ALT1", 3));

        assertThat(CreditAdjustmentCalculator.adjustedTarget(rule, context, 12)).isEqualTo(12);
    }

    private static EvaluationContext context(CourseRecordView... records) {
        TranscriptView transcript = new TranscriptView(
                1L,
                1L,
                100L,
                null,
                null,
                null,
                null,
                2023,
                "단일",
                false,
                0,
                BigDecimal.valueOf(4.0),
                "S1",
                false,
                null,
                null,
                false,
                false,
                false,
                false,
                false,
                List.of(records));
        return new EvaluationContext(transcript, Map.of());
    }

    private static CourseRecordView passed(String courseCode, boolean passed) {
        return new CourseRecordView("2024-1", courseCode, "공교", null, courseCode, 3, passed, false);
    }

    private static GraduationRuleView rule(String... adjustments) {
        String config = "{\"creditAdjustments\":[" + String.join(",", adjustments) + "]}";
        return new GraduationRuleView(1L, "MIN_CREDITS", CourseType.COMMON_GENERAL, "최소 이수량", config);
    }

    private static String adjustment(String requiredCode, String replacementCode, int credits) {
        return adjustment(requiredCode, List.of(replacementCode), credits);
    }

    private static String adjustment(String requiredCode, List<String> replacementCodes, int credits) {
        String replacements = replacementCodes.stream()
                .map(code -> "\"" + code + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"requiredCourseCodes\":[\"" + requiredCode + "\"],"
                + "\"replacementCourseCodes\":[" + replacements + "],"
                + "\"credits\":" + credits + "}";
    }
}
