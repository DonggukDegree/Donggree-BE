package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RuleCategory;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GpaEvaluatorTest extends EvaluatorTestSupport {

    private final GpaEvaluator evaluator = new GpaEvaluator();

    @Test
    void GPA가_최소_평점_이상이면_충족이다() {
        EvaluationContext ctx = context(transcript(0, 2.0, List.of()), Map.of());
        GraduationRuleView rule = rule("{\"minGpa\": 2.0}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isTrue();
    }

    @Test
    void GPA가_최소_평점_미만이면_미충족이다() {
        EvaluationContext ctx = context(transcript(0, 1.99, List.of()), Map.of());
        GraduationRuleView rule = rule("{\"minGpa\": 2.0}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isFalse();
    }

    @Test
    void GPA가_최소_평점보다_높으면_충족이다() {
        EvaluationContext ctx = context(transcript(0, 4.5, List.of()), Map.of());
        GraduationRuleView rule = rule("{\"minGpa\": 2.0}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "GPA", RuleCategory.GRADUATION_REQ, "총 평점평균이 2.0 이상이어야 합니다.", config);
    }
}
