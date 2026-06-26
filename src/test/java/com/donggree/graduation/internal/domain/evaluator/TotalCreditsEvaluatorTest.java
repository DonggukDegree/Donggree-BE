package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TotalCreditsEvaluatorTest extends EvaluatorTestSupport {

    private final TotalCreditsEvaluator evaluator = new TotalCreditsEvaluator();

    @Test
    void 취득학점이_최소학점_이상이면_충족이다() {
        EvaluationContext ctx = context(transcript(130, 3.0, List.of()), Map.of());
        GraduationRuleView rule = rule("{\"minCredits\": 130}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isTrue();
    }

    @Test
    void 취득학점이_최소학점_미만이면_미충족이다() {
        EvaluationContext ctx = context(transcript(129, 3.0, List.of()), Map.of());
        GraduationRuleView rule = rule("{\"minCredits\": 130}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isFalse();
    }

    @Test
    void 결과에_규칙명이_포함된다() {
        EvaluationContext ctx = context(transcript(130, 3.0, List.of()), Map.of());
        GraduationRuleView rule = rule("{\"minCredits\": 130}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.ruleName()).isEqualTo("총 취득학점이 130학점 이상이어야 합니다.");
    }

    @Test
    void supportedTypeName은_TOTAL_CREDITS이다() {
        assertThat(evaluator.supportedTypeName()).isEqualTo("TOTAL_CREDITS");
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "TOTAL_CREDITS", null, "총 취득학점이 130학점 이상이어야 합니다.", config);
    }
}
