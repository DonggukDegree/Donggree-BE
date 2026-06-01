package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
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

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "REQUIRED_COURSE", CourseType.FIRST_MAJOR, "기초프로그래밍은 필수 과목입니다.", config);
    }
}
