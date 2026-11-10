package com.donggree.graduation.internal.domain.evaluator.credits;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.graduation.internal.domain.evaluator.EvaluatorTestSupport;
import com.donggree.graduation.support.EvaluationTestInputs;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GroupedMinCreditsEvaluatorTest extends EvaluatorTestSupport {
    private final MinCreditsEvaluator evaluator = new MinCreditsEvaluator();
    private static final String CONFIG =
            """
            {"creditGroups":[{"courseCodes":["ABC*"],"minCredits":27},
                             {"courseCodes":["DEF*"],"minCredits":27},
                             {"courseCodes":["GHI*"],"minCredits":27},
                             {"courseCodes":["JKL*"],"minCredits":27}],"minSatisfiedGroups":2}
            """;

    @ParameterizedTest
    @CsvSource({"27,27,0,true", "54,0,0,false", "26,26,26,false", "27,0,30,true", "27,27,27,true", "0,0,0,false"})
    void 네_그룹_중_두_그룹의_하한을_각각_충족해야_한다(int a, int b, int c, boolean expected) {
        var records = List.of(
                passed("ABC1001", "A", a, "2024-1"),
                passed("DEF1001", "B", b, "2024-1"),
                passed("GHI1001", "C", c, "2024-1"));
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(CONFIG)),
                                context(transcript(a + b + c, 4.0, records), Map.of()))
                        .satisfied())
                .isEqualTo(expected);
    }

    @Test
    void 한_그룹의_복수_패턴은_OR로_모으고_같은_행을_두번_세지_않는다() {
        String config =
                """
                {"creditGroups":[{"courseCodes":["ABC*","ABC1001","DEV11079"],"minCredits":6},
                                 {"courseCodes":["7777700"],"minCredits":1}],"minSatisfiedGroups":2}
                """;
        var a = passed("ABC1001", "A", 3, "2024-1");
        var exchange = passed("7777700", "교류", 1, "2024-1");
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(config)),
                                context(transcript(4, 4.0, List.of(a, exchange)), Map.of()))
                        .satisfied())
                .isFalse();
        var second = passed("DEV11079", "B", 3, "2024-1");
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(config)),
                                context(transcript(7, 4.0, List.of(a, exchange, second)), Map.of()))
                        .satisfied())
                .isTrue();
    }

    @Test
    void 미이수_과목과_공통_선택자를_벗어난_과목은_각_그룹에서_제외한다() {
        String config =
                """
                {"pdfCourseTypeNames":["전필"],"creditGroups":[{"courseCodes":["ABC*"],"minCredits":3},
                             {"courseCodes":["DEF*"],"minCredits":3}],"minSatisfiedGroups":2}
                """;
        var records = List.of(
                new CourseRecordView("2024-1", "ABC1001", "전필", null, "A", 3, true, false),
                new CourseRecordView("2024-1", "DEF1001", "전필", null, "미이수", 3, false, false),
                new CourseRecordView("2024-1", "DEF1002", "전선", null, "선택 제외", 3, true, false));
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(config)), context(transcript(6, 4.0, records), Map.of()))
                        .satisfied())
                .isFalse();
    }

    @Test
    void 그룹_집계도_주전공과_복수전공의_평가_범위를_유지한다() {
        String config =
                """
                {"courseType":"FIRST_MAJOR","creditGroups":[{"courseCodes":["ABC*"],"minCredits":3},
                             {"courseCodes":["DEF*"],"minCredits":3}],"minSatisfiedGroups":2}
                """;
        var records = List.of(
                new CourseRecordView("2024-1", "ABC1001", "전공", null, "주전공", 3, true, false),
                new CourseRecordView("2024-1", "DEF1001", "복수1", null, "복수1", 3, true, false),
                new CourseRecordView("2024-1", "ABC1002", "복수2", null, "복수2", 3, true, false));
        var classifications = Map.of(
                "ABC1001",
                classification(CourseType.FIRST_MAJOR),
                "ABC1002",
                classification(CourseType.FIRST_MAJOR),
                "DEF1001",
                classification(CourseType.FIRST_MAJOR));
        var transcript = transcript(9, 4.0, records);
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(config)),
                                EvaluationTestInputs.primary(transcript, classifications, MajorRole.DUAL_PRIMARY))
                        .satisfied())
                .isFalse();
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(config)),
                                EvaluationTestInputs.secondary(transcript, classifications, "복수1"))
                        .satisfied())
                .isFalse();
        var secondaryRecords = List.of(
                new CourseRecordView("2024-1", "ABC1001", "복수1", null, "A", 3, true, false),
                new CourseRecordView("2024-1", "DEF1001", "복수1", null, "B", 3, true, false));
        assertThat(evaluator
                        .evaluate(
                                EvaluationTestInputs.rule(rule(config)),
                                EvaluationTestInputs.secondary(
                                        transcript(6, 4.0, secondaryRecords), classifications, "복수1"))
                        .satisfied())
                .isTrue();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "MIN_CREDITS", CourseType.FIRST_MAJOR, "네 그룹 중 두 그룹 이수", config);
    }
}
