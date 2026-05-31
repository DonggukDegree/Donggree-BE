package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScienceExperimentEvaluatorTest extends EvaluatorTestSupport {

    private final ScienceExperimentEvaluator evaluator = new ScienceExperimentEvaluator();

    @Test
    void 실험_과목을_1개_이상_이수하면_충족이다() {
        var records = List.of(passed("SCI001", "일반물리학및실험1", 3, "2023-1"));
        var cls = Map.of("SCI001", classification(1L, CourseType.ACADEMIC_FOUNDATION, "실험", "물리"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(rule("{\"minCount\": 1}"), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 실험_과목이_없으면_미충족이다() {
        var records = List.of(passed("SCI002", "물리학개론", 3, "2023-1"));
        var cls = Map.of("SCI002", classification(2L, CourseType.ACADEMIC_FOUNDATION, "개론", "물리"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(rule("{\"minCount\": 1}"), ctx).satisfied())
                .isFalse();
    }

    @Test
    void 실험_과목_이수_실패는_카운트되지_않는다() {
        var records = List.of(failed("SCI001", "일반물리학및실험1", 3, "2023-1"));
        var cls = Map.of("SCI001", classification(1L, CourseType.ACADEMIC_FOUNDATION, "실험", "물리"));
        EvaluationContext ctx = context(transcript(0, 0.0, records), cls);

        assertThat(evaluator.evaluate(rule("{\"minCount\": 1}"), ctx).satisfied())
                .isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(
                1L, "SCIENCE_EXPERIMENT", CourseType.ACADEMIC_FOUNDATION, "실험 교과목 중 1과목을 필수 선택해야 합니다.", config);
    }
}
