package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RuleCategory;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequiredCourseEvaluatorTest extends EvaluatorTestSupport {

    private final RequiredCourseEvaluator evaluator = new RequiredCourseEvaluator();

    @Test
    void 필수_과목을_이수했으면_충족이다() {
        var records = List.of(passed("CSE1001", "기초프로그래밍", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(3, 4.0, records));
        GraduationRuleView rule = rule("{\"courseName\": \"기초프로그래밍\"}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isTrue();
    }

    @Test
    void 필수_과목을_이수하지_않았으면_미충족이다() {
        EvaluationContext ctx = contextNoClassification(transcript(0, 4.0, List.of()));
        GraduationRuleView rule = rule("{\"courseName\": \"기초프로그래밍\"}");

        RuleResult result = evaluator.evaluate(rule, ctx);

        assertThat(result.satisfied()).isFalse();
    }

    @Test
    void 과목_수강_실패는_이수로_인정되지_않는다() {
        var records = List.of(failed("CSE1001", "기초프로그래밍", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(0, 0.0, records));
        GraduationRuleView rule = rule("{\"courseName\": \"기초프로그래밍\"}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "REQUIRED_COURSE", RuleCategory.MAJOR, "기초프로그래밍은 필수 과목입니다.", config);
    }
}
