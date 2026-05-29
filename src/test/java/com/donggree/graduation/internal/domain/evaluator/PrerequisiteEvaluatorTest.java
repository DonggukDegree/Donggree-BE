package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RuleCategory;
import com.donggree.graduation.internal.domain.EvaluationContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrerequisiteEvaluatorTest extends EvaluatorTestSupport {

    private final PrerequisiteEvaluator evaluator = new PrerequisiteEvaluator();

    private static final String CONFIG = "{\"targetCourseName\": \"자료구조\", \"prerequisiteCourseName\": \"기초프로그래밍\", "
            + "\"conditionField\": null, \"conditionValue\": null}";

    @Test
    void 선이수_과목을_이전_학기에_이수했으면_충족이다() {
        var records = List.of(passed("CSE1001", "기초프로그래밍", 3, "2023-1"), passed("CSE2001", "자료구조", 3, "2023-2"));
        EvaluationContext ctx = contextNoClassification(transcript(6, 4.0, records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 대상_과목을_수강하지_않았으면_충족이다() {
        var records = List.of(passed("CSE1001", "기초프로그래밍", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(3, 4.0, records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isTrue();
    }

    @Test
    void 선이수_과목을_수강하지_않고_대상을_수강했으면_미충족이다() {
        var records = List.of(passed("CSE2001", "자료구조", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(3, 4.0, records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    @Test
    void 선이수와_대상을_같은_학기에_수강하면_미충족이다() {
        var records = List.of(passed("CSE1001", "기초프로그래밍", 3, "2023-1"), passed("CSE2001", "자료구조", 3, "2023-1"));
        EvaluationContext ctx = contextNoClassification(transcript(6, 4.0, records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    @Test
    void 선이수를_나중_학기에_수강하면_미충족이다() {
        var records = List.of(passed("CSE2001", "자료구조", 3, "2023-1"), passed("CSE1001", "기초프로그래밍", 3, "2023-2"));
        EvaluationContext ctx = contextNoClassification(transcript(6, 4.0, records));

        assertThat(evaluator.evaluate(rule(CONFIG), ctx).satisfied()).isFalse();
    }

    @Test
    void 조건_필드가_맞지_않는_학생에게는_규칙이_적용되지_않는다() {
        String configWithCondition = "{\"targetCourseName\": \"EAS1\", \"prerequisiteCourseName\": \"Basic EAS\", "
                + "\"conditionField\": \"englishLevel\", \"conditionValue\": \"S4\"}";
        var records = List.of(passed("ENG001", "EAS1", 2, "2023-1"));
        var t = transcriptWith(0, 4.0, false, null, false, "단일", "S1", records);
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(configWithCondition), ctx).satisfied())
                .isTrue();
    }

    @Test
    void 조건_필드가_맞는_학생에게는_선이수_규칙이_적용된다() {
        String configWithCondition = "{\"targetCourseName\": \"EAS1\", \"prerequisiteCourseName\": \"Basic EAS\", "
                + "\"conditionField\": \"englishLevel\", \"conditionValue\": \"S4\"}";
        var records = List.of(passed("ENG001", "EAS1", 2, "2023-1"));
        var t = transcriptWith(0, 4.0, false, null, false, "단일", "S4", records);
        EvaluationContext ctx = contextNoClassification(t);

        assertThat(evaluator.evaluate(rule(configWithCondition), ctx).satisfied())
                .isFalse();
    }

    private GraduationRuleView rule(String config) {
        return new GraduationRuleView(1L, "PREREQUISITE", RuleCategory.MAJOR, "자료구조 이전에 기초프로그래밍을 선이수해야 합니다.", config);
    }
}
