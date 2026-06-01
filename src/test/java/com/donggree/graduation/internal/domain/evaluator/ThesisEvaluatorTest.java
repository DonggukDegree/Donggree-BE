package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
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

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "THESIS", null, "종합설계1과 종합설계2 또는 개별연구를 이수해야 합니다.", config);
    }
}
