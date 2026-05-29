package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RuleCategory;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThesisEvaluatorTest extends EvaluatorTestSupport {

    private final ThesisEvaluator evaluator = new ThesisEvaluator();

    private static final String CONFIG = "{\"exemptStudentTypes\": [\"학석사연계과정\"],"
            + "\"requiredCourseSets\": [[\"종합설계1\", \"종합설계2\"], [\"종합설계1\", \"개별연구\"]]}";

    @Test
    void 종합설계1과_2를_이수하면_충족이다() {
        var records = List.of(passed("CSE4001", "종합설계1", 3, "2024-1"), passed("CSE4002", "종합설계2", 3, "2024-2"));
        var t = transcriptWith(6, 4.0, false, null, false, "단일", "S1", records);
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 종합설계1과_개별연구를_이수하면_충족이다() {
        var records = List.of(passed("CSE4001", "종합설계1", 3, "2024-1"), passed("CSE4003", "개별연구", 3, "2024-2"));
        var t = transcriptWith(6, 4.0, false, null, false, "단일", "S1", records);
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 종합설계1만_이수하면_미충족이다() {
        var records = List.of(passed("CSE4001", "종합설계1", 3, "2024-1"));
        var t = transcriptWith(3, 4.0, false, null, false, "단일", "S1", records);
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    @Test
    void 학석사연계과정_학생은_면제_처리된다() {
        var t = transcriptWith(0, 4.0, false, null, false, "학석사연계과정", "S1", List.of());
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 아무_과목도_이수하지_않으면_미충족이다() {
        var t = transcriptWith(0, 4.0, false, null, false, "단일", "S1", List.of());
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(
                1L, "THESIS", RuleCategory.GRADUATION_REQ, "종합설계1과 종합설계2 또는 개별연구를 이수해야 합니다.", config);
    }
}
