package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RuleCategory;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnglishCourseEvaluatorTest extends EvaluatorTestSupport {

    private final EnglishCourseEvaluator evaluator = new EnglishCourseEvaluator();

    @Test
    void 영어강의_대상자가_아니면_자동_충족이다() {
        var t = transcriptWith(0, 4.0, false, null, false, "단일", "S1", List.of());
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx).satisfied())
                .isTrue();
    }

    @Test
    void PDF_이수_결과가_true이면_자동_충족이다() {
        var t = transcriptWith(0, 4.0, true, true, false, "단일", "S1", List.of());
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 전공_영어강의를_충분히_이수하면_충족이다() {
        var records = List.of(
                passed("MAJOR001", "<영어>알고리즘", 3, "2023-1"),
                passed("MAJOR002", "<영어>자료구조", 3, "2023-2")
        );
        var cls = Map.of(
                "MAJOR001", new CourseClassificationView(1L, CourseType.FIRST_MAJOR, null, null, null),
                "MAJOR002", new CourseClassificationView(2L, CourseType.FIRST_MAJOR, null, null, null)
        );
        var t = transcriptWith(6, 4.0, true, null, false, "단일", "S1", records);
        EvaluationContext ctx = context(t, cls);

        assertThat(evaluator.evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 전공_영어강의가_부족하면_미충족이다() {
        var records = List.of(passed("MAJOR001", "<영어>알고리즘", 3, "2023-1"));
        var cls = Map.of("MAJOR001", new CourseClassificationView(1L, CourseType.FIRST_MAJOR, null, null, null));
        var t = transcriptWith(3, 4.0, true, null, false, "단일", "S1", records);
        EvaluationContext ctx = context(t, cls);

        assertThat(evaluator.evaluate(rule("{\"courseTypes\": [\"FIRST_MAJOR\"], \"minCount\": 2}"), ctx).satisfied())
                .isFalse();
    }

    @Test
    void courseType_null이면_전체_영어강의를_합산한다() {
        var records = List.of(
                passed("GEN001", "<영어>교양영어", 2, "2023-1"),
                passed("MAJOR001", "<영어>알고리즘", 3, "2023-2"),
                passed("GEN002", "<영어>글쓰기", 2, "2024-1"),
                passed("MAJOR002", "<영어>자료구조", 3, "2024-2")
        );
        var cls = Map.of(
                "GEN001", new CourseClassificationView(1L, CourseType.COMMON_GENERAL, null, null, null),
                "MAJOR001", new CourseClassificationView(2L, CourseType.FIRST_MAJOR, null, null, null),
                "GEN002", new CourseClassificationView(3L, CourseType.LIBERAL_ARTS, null, null, null),
                "MAJOR002", new CourseClassificationView(4L, CourseType.FIRST_MAJOR, null, null, null)
        );
        var t = transcriptWith(10, 4.0, true, null, false, "단일", "S1", records);
        EvaluationContext ctx = context(t, cls);

        assertThat(evaluator.evaluate(rule("{\"courseTypes\": null, \"minCount\": 4}"), ctx).satisfied())
                .isTrue();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "ENGLISH_COURSE", RuleCategory.GRADUATION_REQ,
                "영어강의 이수 규칙", config);
    }
}
