package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.MajorRole;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThesisEvaluatorTest extends EvaluatorTestSupport {

    private final ThesisEvaluator evaluator = new ThesisEvaluator();

    // 종합설계1: ["CSE4066","CSC4018"], 종합설계2: ["CSE4067","CSC4019"], 개별연구: DAI* prefix
    private static final String CONFIG = "{\"exemptStudentTypes\":[\"학석사연계과정\"],"
            + "\"requiredCourseSets\":["
            + "[[\"CSE4066\",\"CSC4018\"],[\"CSE4067\",\"CSC4019\"]],"
            + "[[\"CSE4066\",\"CSC4018\"],[\"DAI*\"]]"
            + "]}";

    @Test
    void 종합설계1과_2를_이수하면_충족이다() {
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"), passed("CSE4067", "종합설계2", 3, "2024-2"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(6, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 동일유사_교과목_다른_코드로도_충족이다() {
        var records = List.of(passed("CSC4018", "종합설계1", 3, "2024-1"), passed("CSC4019", "종합설계2", 3, "2024-2"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(6, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 종합설계1과_개별연구를_이수하면_충족이다() {
        // DAI로 시작하는 학수번호는 모두 개별연구로 인정
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"), passed("DAI4001", "개별연구", 3, "2024-2"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(6, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void DAI_접두사_학수번호면_개별연구로_인정된다() {
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"), passed("DAI9999", "개별연구심화", 3, "2024-2"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(6, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 종합설계1만_이수하면_미충족이다() {
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(3, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    @Test
    void 학석사연계과정_학생은_면제_처리된다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "학석사연계과정", "S1", List.of()));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 아무_과목도_이수하지_않으면_미충족이다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "단일", "S1", List.of()));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    // --- requiredCourseSets가 없으면 졸업논문심사(PDF) 결과로 판정 ---

    @Test
    void 지정_과목이_없는_규칙은_졸업논문심사_합격이면_충족이다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, true, "단일", "S1", List.of()));

        assertThat(evaluator.evaluate(rule("{}"), ctx).satisfied()).isTrue();
    }

    @Test
    void 지정_과목이_없는_규칙은_졸업논문심사_불합격이면_미충족이다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "단일", "S1", List.of()));

        assertThat(evaluator.evaluate(rule("{}"), ctx).satisfied()).isFalse();
    }

    @Test
    void 지정_과목이_없어도_면제_대상이면_충족이다() {
        String config = "{\"exemptStudentTypes\":[\"학석사연계과정\"]}";
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "학석사연계과정", "S1", List.of()));

        assertThat(evaluator.evaluate(rule(config), ctx).satisfied()).isTrue();
    }

    /** 지정 과목이 있는 규칙은 과목 이수만 본다 — 논문심사 결과에 영향받지 않는다. */
    @Test
    void 지정_과목이_있는_규칙은_논문심사_합격이어도_과목_이수만_본다() {
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, true, "단일", "S1", List.of()));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    // --- 부분 면제: 면제 대상이어도 지정 과목만 이수한 것으로 간주한다 ---

    // 컴퓨터·AI학부 심화과정. 학석사연계과정은 종합설계2·개별연구만 면제되고 종합설계1은 그대로 요구된다.
    private static final String PARTIAL_EXEMPT_CONFIG = "{\"exemptStudentTypes\":[\"학석사연계과정\"],"
            + "\"exemptCourseCodes\":[\"CSE4067\",\"CSC4019\",\"DAI*\"],"
            + "\"requiredCourseSets\":["
            + "[[\"CSE4066\",\"CSC4018\"],[\"CSE4067\",\"CSC4019\"]],"
            + "[[\"CSE4066\",\"CSC4018\"],[\"DAI*\"]]"
            + "]}";

    @Test
    void 부분면제_대상이_종합설계1을_이수하면_충족이다() {
        // 종합설계2·개별연구는 면제되므로 종합설계1만 이수하면 된다.
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(3, 4.0, false, null, false, "학석사연계과정", "S1", records));

        assertThat(evaluator.evaluate(rule(PARTIAL_EXEMPT_CONFIG), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 부분면제_대상이어도_종합설계1을_이수하지_않으면_미충족이다() {
        // 전체 면제가 아니라는 점이 핵심이다.
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(0, 4.0, false, null, false, "학석사연계과정", "S1", List.of()));

        assertThat(evaluator.evaluate(rule(PARTIAL_EXEMPT_CONFIG), ctx).satisfied())
                .isFalse();
    }

    @Test
    void 부분면제_대상이_동일유사_코드로_종합설계1을_이수해도_충족이다() {
        var records = List.of(passed("CSC4018", "종합설계1", 3, "2024-1"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(3, 4.0, false, null, false, "학석사연계과정", "S1", records));

        assertThat(evaluator.evaluate(rule(PARTIAL_EXEMPT_CONFIG), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 부분면제_대상이_아닌_학생은_면제_과목_목록의_영향을_받지_않는다() {
        // 일반과정 학생은 종합설계1만으로는 여전히 미충족이다.
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(3, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(PARTIAL_EXEMPT_CONFIG), ctx).satisfied())
                .isFalse();
    }

    @Test
    void 부분면제_대상이_아닌_학생은_종합설계1과_2를_모두_이수해야_충족이다() {
        var records = List.of(passed("CSE4066", "종합설계1", 3, "2024-1"), passed("CSE4067", "종합설계2", 3, "2024-2"));
        EvaluationContext ctx =
                contextNoClassification(transcriptWith(6, 4.0, false, null, false, "단일", "S1", records));

        assertThat(evaluator.evaluate(rule(PARTIAL_EXEMPT_CONFIG), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 주전공_합격을_복수전공_합격으로_재사용하지_않는다() {
        var transcript = transcriptWithThesisStatuses(true, false, "학사과정", List.of());

        assertThat(evaluator
                        .evaluate(rule("{}"), EvaluationContext.primary(transcript, Map.of(), MajorRole.DUAL_PRIMARY))
                        .satisfied())
                .isTrue();
        assertThat(evaluator
                        .evaluate(rule("{}"), EvaluationContext.secondary(transcript, Map.of(), "복수1"))
                        .satisfied())
                .isFalse();
    }

    @Test
    void 복수전공_합격을_주전공_합격으로_재사용하지_않는다() {
        var transcript = transcriptWithThesisStatuses(false, true, "학사과정", List.of());

        assertThat(evaluator
                        .evaluate(rule("{}"), EvaluationContext.primary(transcript, Map.of(), MajorRole.DUAL_PRIMARY))
                        .satisfied())
                .isFalse();
        assertThat(evaluator
                        .evaluate(rule("{}"), EvaluationContext.secondary(transcript, Map.of(), "복수1"))
                        .satisfied())
                .isTrue();
    }

    @Test
    void 복수전공_과목_판정에는_복수1_과목만_사용한다() {
        var primaryCourse = new CourseRecordView("2024-1", "CSE4066", "전공", "전문", "설계", 3, true, false);
        var dual2Course = new CourseRecordView("2024-1", "CSE4066", "복수2", "전문", "설계", 3, true, false);
        var secondaryCourse = new CourseRecordView("2024-1", "CSE4066", "복수1", "전문", "설계", 3, true, false);
        var configuredRule = rule("{\"requiredCourseSets\":[[[\"CSE4066\"]]]}");

        for (CourseRecordView course : List.of(primaryCourse, dual2Course, secondaryCourse)) {
            var transcript = transcriptWithThesisStatuses(true, true, "학사과정", List.of(course));
            var context = EvaluationContext.secondary(transcript, Map.of(), "복수1");

            assertThat(evaluator.evaluate(configuredRule, context).satisfied()).isEqualTo(course == secondaryCourse);
        }
    }

    @Test
    void 복수전공에도_전체_면제를_적용한다() {
        var transcript = transcriptWithThesisStatuses(false, false, "학석사연계과정", List.of());
        var context = EvaluationContext.secondary(transcript, Map.of(), "복수1");

        assertThat(evaluator.evaluate(rule(CONFIG), context).satisfied()).isTrue();
    }

    @Test
    void 복수전공_부분_면제도_나머지_복수1_과목_이수를_요구한다() {
        var course = new CourseRecordView("2024-1", "CSE4066", "복수1", "전문", "설계", 3, true, false);
        var withCourse = transcriptWithThesisStatuses(false, false, "학석사연계과정", List.of(course));
        var withoutCourse = transcriptWithThesisStatuses(true, true, "학석사연계과정", List.of());

        assertThat(evaluator
                        .evaluate(rule(PARTIAL_EXEMPT_CONFIG), EvaluationContext.secondary(withCourse, Map.of(), "복수1"))
                        .satisfied())
                .isTrue();
        assertThat(evaluator
                        .evaluate(
                                rule(PARTIAL_EXEMPT_CONFIG),
                                EvaluationContext.secondary(withoutCourse, Map.of(), "복수1"))
                        .satisfied())
                .isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "THESIS", null, "종합설계1과 종합설계2 또는 개별연구를 이수해야 합니다.", config);
    }
}
