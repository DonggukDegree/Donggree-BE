package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MinAreaCreditsEvaluatorTest extends EvaluatorTestSupport {

    private final MinAreaCreditsEvaluator evaluator = new MinAreaCreditsEvaluator();

    @Test
    void 공통교양_학점이_최소_이상이면_충족이다() {
        var records = List.of(
                passed("GEN001", "불교와인간", 2, "2023-1"),
                passed("GEN002", "동국인성", 3, "2023-1"),
                passed("GEN003", "채플", 0, "2023-1"),
                passed("GEN004", "원불교", 3, "2023-2"),
                passed("GEN005", "EAS1", 3, "2023-2"),
                passed("GEN006", "EAS2", 3, "2024-1"),
                passed("GEN007", "어드벤처디자인", 3, "2024-1"));
        var cls = Map.of(
                "GEN001", classification(1L, CourseType.COMMON_GENERAL),
                "GEN002", classification(2L, CourseType.COMMON_GENERAL),
                "GEN003", classification(3L, CourseType.COMMON_GENERAL),
                "GEN004", classification(4L, CourseType.COMMON_GENERAL),
                "GEN005", classification(5L, CourseType.COMMON_GENERAL),
                "GEN006", classification(6L, CourseType.COMMON_GENERAL),
                "GEN007", classification(7L, CourseType.COMMON_GENERAL));
        EvaluationContext ctx = context(transcript(17, 4.0, records), cls);
        GraduationRuleView rule =
                rule("{\"courseType\": \"COMMON_GENERAL\", \"subCategories\": null, \"minCredits\": 17}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
    }

    @Test
    void 공통교양_학점이_부족하면_미충족이다() {
        var records = List.of(passed("GEN001", "불교와인간", 2, "2023-1"));
        var cls = Map.of("GEN001", classification(1L, CourseType.COMMON_GENERAL));
        EvaluationContext ctx = context(transcript(2, 4.0, records), cls);
        GraduationRuleView rule =
                rule("{\"courseType\": \"COMMON_GENERAL\", \"subCategories\": null, \"minCredits\": 17}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    @Test
    void subCategory_필터링으로_기본소양만_집계한다() {
        var records = List.of(
                passed("BSM001", "미적분학및연습1", 3, "2023-1"),
                passed("BSM002", "물리학개론", 3, "2023-1"),
                passed("BSM003", "기술과문명", 3, "2023-2") // 기본소양
                );
        var cls = Map.of(
                "BSM001", new CourseClassificationView(1L, CourseType.ACADEMIC_FOUNDATION, null, "수학", null),
                "BSM002", new CourseClassificationView(2L, CourseType.ACADEMIC_FOUNDATION, null, "과학", null),
                "BSM003", new CourseClassificationView(3L, CourseType.ACADEMIC_FOUNDATION, null, "기본소양", null));
        EvaluationContext ctx = context(transcript(9, 4.0, records), cls);
        GraduationRuleView rule =
                rule("{\"courseType\": \"ACADEMIC_FOUNDATION\", \"subCategories\": [\"기본소양\"], \"minCredits\": 3}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isTrue();
    }

    @Test
    void 이수_실패_과목은_학점에_포함되지_않는다() {
        var records = List.of(failed("MAJOR001", "자료구조", 3, "2023-1"));
        var cls = Map.of("MAJOR001", classification(1L, CourseType.FIRST_MAJOR));
        EvaluationContext ctx = context(transcript(0, 0.0, records), cls);
        GraduationRuleView rule =
                rule("{\"courseType\": \"FIRST_MAJOR\", \"subCategories\": null, \"minCredits\": 60}");

        assertThat(evaluator.evaluate(rule, ctx).satisfied()).isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "MIN_AREA_CREDITS", CourseType.COMMON_GENERAL, "테스트 규칙", config);
    }
}
