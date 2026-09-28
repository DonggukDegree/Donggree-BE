package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RequiredCourseEvaluatorTest extends EvaluatorTestSupport {

    private final RequiredCourseEvaluator evaluator = new RequiredCourseEvaluator();

    private static final String EXPERIMENT_SETS =
            """
            {"requiredCourseSets":[[["PHY1","OLD_PHY1"],["PHY2"]],[["BIO1"],["BIO2"]]]}
            """;

    @ParameterizedTest
    @CsvSource({
        "PHY1,PHY2,true",
        "OLD_PHY1,PHY2,true",
        "BIO1,BIO2,true",
        "PHY1,BIO2,false",
        "BIO1,PHY2,false",
        "PHY1,PHY1,false"
    })
    void 한_세트의_모든_그룹을_이수해야_충족이다(String first, String second, boolean expected) {
        var records = List.of(passed(first, first, 3, "2023-1"), passed(second, second, 3, "2023-2"));

        assertThat(evaluator
                        .evaluate(rule(EXPERIMENT_SETS), contextNoClassification(transcript(6, 4.0, records)))
                        .satisfied())
                .isEqualTo(expected);
    }

    @Test
    void 완성한_세트_외의_추가_과목은_충족을_방해하지_않는다() {
        var records = List.of(
                passed("PHY1", "물리1", 3, "2023-1"),
                passed("PHY2", "물리2", 3, "2023-1"),
                passed("BIO1", "생물1", 3, "2023-2"));
        assertThat(evaluator
                        .evaluate(rule(EXPERIMENT_SETS), contextNoClassification(transcript(9, 4.0, records)))
                        .satisfied())
                .isTrue();
    }

    @Test
    void 미이수나_실패한_과목이_있는_세트는_충족하지_않는다() {
        for (var records : List.of(
                List.<CourseRecordView>of(),
                List.of(passed("PHY1", "물리1", 3, "2023-1")),
                List.of(passed("PHY1", "물리1", 3, "2023-1"), failed("PHY2", "물리2", 3, "2023-2")))) {
            assertThat(evaluator
                            .evaluate(rule(EXPERIMENT_SETS), contextNoClassification(transcript(3, 4.0, records)))
                            .satisfied())
                    .isFalse();
        }
    }

    @Test
    void 과목_세트에서도_접두어_일치를_지원한다() {
        var records = List.of(passed("PHY1001", "물리1", 3, "2023-1"), passed("PHY2001", "물리2", 3, "2023-2"));
        var rule = rule("{\"requiredCourseSets\":[[[\"PHY1*\"],[\"PHY2*\"]]]}");
        assertThat(evaluator
                        .evaluate(rule, contextNoClassification(transcript(6, 4.0, records)))
                        .satisfied())
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "ACADEMIC_FOUNDATION,학기,true",
        "ACADEMIC_FOUNDATION,공교,true",
        "ACADEMIC_FOUNDATION,복수2,false",
        "COMMON_GENERAL,공교,true",
        "FIRST_MAJOR,전공,false",
        "FIRST_MAJOR,복수1,true",
        "FIRST_MAJOR,복수2,false"
    })
    void 복수전공_과목_세트도_기존_필수과목의_인정_범위를_유지한다(CourseType type, String pdfType, boolean expected) {
        var records = List.of(
                new CourseRecordView("2023-1", "PHY1", pdfType, "과학", "물리1", 3, true, false),
                new CourseRecordView("2023-2", "PHY2", pdfType, "과학", "물리2", 3, true, false));
        var ctx = EvaluationContext.secondary(transcript(6, 4.0, records), Map.of(), "복수1");
        var rule = new GraduationRuleView(1L, "REQUIRED_COURSE", type, "실험 세트", EXPERIMENT_SETS);
        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[[]]", "[[[]]]", "[[null]]", "[null]"})
    void 비어있는_세트는_자동_충족으로_판정하지_않는다(String sets) {
        assertThat(evaluator
                        .evaluate(
                                rule("{\"requiredCourseSets\":" + sets + "}"),
                                contextNoClassification(transcript(0, 4.0, List.of())))
                        .satisfied())
                .isFalse();
    }

    @Test
    void 과목_세트에서도_영어레벨_면제와_적용_대상을_유지한다() {
        var ctx = contextNoClassification(transcriptWith(0, 4.0, false, null, false, "단일", "S0", List.of()));
        for (String condition : List.of("\"exemptEnglishLevels\":[\"S0\"]", "\"requiredEnglishLevels\":[\"S4\"]")) {
            var rule = rule("{\"requiredCourseSets\":[[[\"A\"],[\"B\"]]]," + condition + "}");
            assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
        }
    }

    @Test
    void 필수_과목을_이수했으면_충족이다() {
        var records = List.of(passed("CSE1001", "기초프로그래밍", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(3, 4.0, records));
        GraduationRuleView rule = rule("{\"courseCodes\": [\"CSE1001\"]}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isTrue();
    }

    @Test
    void 필수_과목을_이수하지_않았으면_미충족이다() {
        EvaluationContext ctx = contextNoClassification(transcript(0, 4.0, List.of()));

        assertThat(evaluator
                        .evaluate(rule("{\"courseCodes\": [\"CSE1001\"]}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 과목_수강_실패는_이수로_인정되지_않는다() {
        var records = List.of(failed("CSE1001", "기초프로그래밍", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(0, 0.0, records));

        assertThat(evaluator
                        .evaluate(rule("{\"courseCodes\": [\"CSE1001\"]}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 동일유사_교과목_배열에서_다른_코드를_이수해도_충족이다() {
        var records = List.of(passed("CSE1001-OLD", "프로그래밍기초", 3, "2021-1"));
        EvaluationContext ctx = contextNoClassification(transcript(3, 4.0, records));

        assertThat(evaluator
                        .evaluate(rule("{\"courseCodes\": [\"CSE1001\", \"CSE1001-OLD\"]}"), ctx)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 면제_영어레벨_학생은_과목을_이수하지_않아도_충족이다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "단일", "S0", List.of()));
        GraduationRuleView easRule = new GraduationRuleView(
                2L,
                "REQUIRED_COURSE",
                CourseType.COMMON_GENERAL,
                "EAS1은 필수 과목입니다.",
                "{\"courseCodes\":[\"RGC1080\"],\"exemptEnglishLevels\":[\"S0\"]}");

        assertThat(evaluator.evaluate(easRule, ctx).satisfied()).isTrue();
    }

    @Test
    void 면제_영어레벨_아닌_학생은_과목을_이수해야_충족이다() {
        var records = List.of(passed("RGC1080", "EAS1", 2, "2023-1"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(2, 4.0, false, null, false, "단일", "S1", records));
        GraduationRuleView easRule = new GraduationRuleView(
                2L,
                "REQUIRED_COURSE",
                CourseType.COMMON_GENERAL,
                "EAS1은 필수 과목입니다.",
                "{\"courseCodes\":[\"RGC1080\"],\"exemptEnglishLevels\":[\"S0\"]}");

        assertThat(evaluator.evaluate(easRule, ctx).satisfied()).isTrue();
    }

    @Test
    void 적용_대상_영어레벨이_아닌_학생은_BasicEAS_이수_없이도_충족이다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "단일", "S1", List.of()));
        GraduationRuleView basicEasRule = new GraduationRuleView(
                3L,
                "REQUIRED_COURSE",
                CourseType.COMMON_GENERAL,
                "BasicEAS는 필수 과목입니다.",
                "{\"courseCodes\":[\"RGC1030\"],\"requiredEnglishLevels\":[\"S4\"]}");

        assertThat(evaluator.evaluate(basicEasRule, ctx).satisfied()).isTrue();
    }

    @Test
    void S4_학생은_BasicEAS를_이수해야_충족이다() {
        var records = List.of(passed("RGC1030", "BasicEAS", 2, "2023-1"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(2, 4.0, false, null, false, "단일", "S4", records));
        GraduationRuleView basicEasRule = new GraduationRuleView(
                3L,
                "REQUIRED_COURSE",
                CourseType.COMMON_GENERAL,
                "BasicEAS는 필수 과목입니다.",
                "{\"courseCodes\":[\"RGC1030\"],\"requiredEnglishLevels\":[\"S4\"]}");

        assertThat(evaluator.evaluate(basicEasRule, ctx).satisfied()).isTrue();
    }

    @Test
    void S4_학생이_BasicEAS를_이수하지_않으면_미충족이다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "단일", "S4", List.of()));
        GraduationRuleView basicEasRule = new GraduationRuleView(
                3L,
                "REQUIRED_COURSE",
                CourseType.COMMON_GENERAL,
                "BasicEAS는 필수 과목입니다.",
                "{\"courseCodes\":[\"RGC1030\"],\"requiredEnglishLevels\":[\"S4\"]}");

        assertThat(evaluator.evaluate(basicEasRule, ctx).satisfied()).isFalse();
    }

    @Test
    void 복수전공_필수과목은_해당_복수_순번에서_이수해야_한다() {
        var records = List.of(new CourseRecordView("2024-1", "DAI1001", "복수2", "전문", "인공지능", 3, true, false));
        EvaluationContext ctx = EvaluationContext.secondary(transcript(3, 4.0, records), Map.of(), "복수1");

        assertThat(evaluator
                        .evaluate(rule("{\"courseCodes\":[\"DAI1001\"]}"), ctx)
                        .satisfied())
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "COMMON_GENERAL,공교,true",
        "COMMON_GENERAL,학기,true",
        "COMMON_GENERAL,일교,true",
        "COMMON_GENERAL,전공,true",
        "COMMON_GENERAL,복수1,true",
        "COMMON_GENERAL,복수2,false",
        "ACADEMIC_FOUNDATION,학기,true",
        "ACADEMIC_FOUNDATION,복수2,false",
        "FIRST_MAJOR,전공,false",
        "FIRST_MAJOR,복수1,true",
        "SECOND_MAJOR,공교,false",
        "SECOND_MAJOR,복수1,true",
        "LIBERAL_ARTS,일교,false",
        ",공교,false"
    })
    void 복수전공_필수과목의_규칙별_인정_범위를_구분한다(CourseType ruleCourseType, String pdfCourseType, boolean expected) {
        var records = List.of(new CourseRecordView("2024-1", "RGC1001", pdfCourseType, null, "지정과목", 3, true, false));
        EvaluationContext ctx = EvaluationContext.secondary(transcript(3, 4.0, records), Map.of(), "복수1");
        GraduationRuleView requiredRule = new GraduationRuleView(
                2L, "REQUIRED_COURSE", ruleCourseType, "지정과목은 필수", "{\"courseCodes\":[\"RGC1001\"]}");

        assertThat(evaluator.evaluate(requiredRule, ctx).satisfied()).isEqualTo(expected);
    }

    @Test
    void 복수전공_공통교양_필수는_이수_실패나_다른_학수번호를_인정하지_않는다() {
        var records = List.of(
                new CourseRecordView("2024-1", "RGC1001", "공교", "글", "글쓰기", 3, false, false),
                new CourseRecordView("2024-1", "RGC1002", "공교", "글", "다른글쓰기", 3, true, false));
        EvaluationContext ctx = EvaluationContext.secondary(transcript(3, 4.0, records), Map.of(), "복수1");
        GraduationRuleView requiredRule = new GraduationRuleView(
                2L, "REQUIRED_COURSE", CourseType.COMMON_GENERAL, "글쓰기는 필수", "{\"courseCodes\":[\"RGC1001\"]}");

        assertThat(evaluator.evaluate(requiredRule, ctx).satisfied()).isFalse();
    }

    @Test
    void 복수전공_공통교양_필수의_동일유사_학수번호와_접두어를_인정한다() {
        var records = List.of(new CourseRecordView("2024-1", "RGC1001", "공교", "글", "글쓰기", 3, true, false));
        EvaluationContext ctx = EvaluationContext.secondary(transcript(3, 4.0, records), Map.of(), "복수1");
        for (String codes : List.of("[\"RGC1002\",\"RGC1001\"]", "[\"RGC*\"]")) {
            GraduationRuleView requiredRule = new GraduationRuleView(
                    2L, "REQUIRED_COURSE", CourseType.COMMON_GENERAL, "글쓰기는 필수", "{\"courseCodes\":" + codes + "}");

            assertThat(evaluator.evaluate(requiredRule, ctx).satisfied()).isTrue();
        }
    }

    @Test
    void 복수전공_공통교양_필수_인정이_최소학점_규칙의_기존_범위를_넓히지_않는다() {
        var records = List.of(new CourseRecordView("2024-1", "RGC1001", "공교", "글", "글쓰기", 3, true, false));
        EvaluationContext ctx = EvaluationContext.secondary(transcript(3, 4.0, records), Map.of(), "복수1");
        GraduationRuleView requiredRule = new GraduationRuleView(
                2L, "REQUIRED_COURSE", CourseType.COMMON_GENERAL, "글쓰기는 필수", "{\"courseCodes\":[\"RGC1001\"]}");
        GraduationRuleView creditRule = new GraduationRuleView(
                3L,
                "MIN_CREDITS",
                CourseType.COMMON_GENERAL,
                "글쓰기 3학점",
                "{\"courseCodes\":[\"RGC1001\"],\"minCredits\":3}");

        assertThat(evaluator.evaluate(requiredRule, ctx).satisfied()).isTrue();
        assertThat(new MinCreditsEvaluator().evaluate(creditRule, ctx).satisfied())
                .isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "REQUIRED_COURSE", CourseType.FIRST_MAJOR, "기초프로그래밍은 필수 과목입니다.", config);
    }
}
