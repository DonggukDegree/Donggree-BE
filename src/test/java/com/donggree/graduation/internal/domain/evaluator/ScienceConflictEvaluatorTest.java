package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScienceConflictEvaluatorTest extends EvaluatorTestSupport {

    private final ScienceConflictEvaluator evaluator = new ScienceConflictEvaluator();

    @Test
    void 같은_subject_domain의_실험과_개론을_이수하면_미충족이다() {
        var records = List.of(passed("SCI001", "일반물리학및실험1", 3, "2023-1"), passed("SCI002", "물리학개론", 3, "2023-2"));
        var cls = Map.of(
                "SCI001", classification(1L, CourseType.ACADEMIC_FOUNDATION, "실험", "물리"),
                "SCI002", classification(2L, CourseType.ACADEMIC_FOUNDATION, "개론", "물리"));
        EvaluationContext ctx = context(transcript(6, 4.0, records), cls);

        assertThat(evaluator.evaluate(rule("{}"), ctx).satisfied()).isFalse();
    }

    @Test
    void 다른_subject_domain의_실험과_개론은_충돌하지_않는다() {
        var records = List.of(passed("SCI001", "일반물리학및실험1", 3, "2023-1"), passed("SCI002", "생물학개론", 3, "2023-2"));
        var cls = Map.of(
                "SCI001", classification(1L, CourseType.ACADEMIC_FOUNDATION, "실험", "물리"),
                "SCI002", classification(2L, CourseType.ACADEMIC_FOUNDATION, "개론", "생물"));
        EvaluationContext ctx = context(transcript(6, 4.0, records), cls);

        assertThat(evaluator.evaluate(rule("{}"), ctx).satisfied()).isTrue();
    }

    @Test
    void 실험만_이수하고_개론이_없으면_충족이다() {
        var records = List.of(passed("SCI001", "일반물리학및실험1", 3, "2023-1"));
        var cls = Map.of("SCI001", classification(1L, CourseType.ACADEMIC_FOUNDATION, "실험", "물리"));
        EvaluationContext ctx = context(transcript(3, 4.0, records), cls);

        assertThat(evaluator.evaluate(rule("{}"), ctx).satisfied()).isTrue();
    }

    @Test
    void 수강_이력이_없으면_충족이다() {
        EvaluationContext ctx = context(transcript(0, 4.0, List.of()), Map.of());

        assertThat(evaluator.evaluate(rule("{}"), ctx).satisfied()).isTrue();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(
                1L, "SCIENCE_CONFLICT", CourseType.ACADEMIC_FOUNDATION, "동일한 실험 교과목과 개론 교과목을 이수할 수 없습니다.", config);
    }
}
