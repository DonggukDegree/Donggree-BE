package com.donggree.graduation.internal.domain.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.transcript.CourseRecordView;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SecondaryAcademicFoundationTest {

    private final MinCreditsEvaluator minCredits = new MinCreditsEvaluator();
    private final RequiredCourseEvaluator requiredCourse = new RequiredCourseEvaluator();

    @Test
    void 복수전공_학문기초_학점은_분류와_선택자로_집계한다() {
        var context = context(
                record("MAT1001", "학기", true),
                record("PHY1001", "학기", true),
                record("MAT1002", "학기", false),
                record("CSE1001", "전공", true),
                record("MAT1003", "복수2", true));
        var config =
                """
                {"courseType":"ACADEMIC_FOUNDATION","areaNames":["수학"],"minCredits":3}
                """;

        assertThat(minCredits.evaluate(rule("MIN_CREDITS", config), context).satisfied())
                .isTrue();
        assertThat(minCredits
                        .evaluate(rule("MIN_CREDITS", config.replace(":3", ":6")), context)
                        .satisfied())
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"학기", "일교", "복수1"})
    void 복수전공_학문기초_필수과목은_학수번호로_확인한다(String pdfType) {
        var context = context(record("MAT1001", pdfType, true));

        assertThat(requiredCourse
                        .evaluate(rule("REQUIRED_COURSE", "{\"courseCodes\":[\"MAT*\"]}"), context)
                        .satisfied())
                .isTrue();
    }

    @Test
    void 미이수와_복수2_학문기초_필수과목은_인정하지_않는다() {
        var context = context(record("MAT1001", "학기", false), record("MAT1003", "복수2", true));

        assertThat(requiredCourse
                        .evaluate(rule("REQUIRED_COURSE", "{\"courseCodes\":[\"MAT*\"]}"), context)
                        .satisfied())
                .isFalse();
    }

    @Test
    void 학문기초_범위_확장으로_전공_학점과_전공_필수가_충족되지_않는다() {
        var context = context(record("MAT1001", "학기", true), record("CSE1001", "전공", true));
        var minRule = new GraduationRuleView(
                1L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "전공 3학점",
                "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":3}");
        var requiredRule = new GraduationRuleView(
                2L, "REQUIRED_COURSE", CourseType.FIRST_MAJOR, "전공 필수", "{\"courseCodes\":[\"CSE1001\"]}");

        assertThat(minCredits.evaluate(minRule, context).satisfied()).isFalse();
        assertThat(requiredCourse.evaluate(requiredRule, context).satisfied()).isFalse();
    }

    private GraduationRuleView rule(String typeName, String config) {
        return new GraduationRuleView(1L, typeName, CourseType.ACADEMIC_FOUNDATION, "학문기초 규칙", config);
    }

    private CourseRecordView record(String code, String pdfType, boolean passed) {
        return new CourseRecordView("2023-1", code, pdfType, null, code, 3, passed, false);
    }

    private EvaluationContext context(CourseRecordView... records) {
        var transcript = EvaluatorTestSupport.transcriptWithThesisStatuses(false, false, "단일", List.of(records));
        Map<String, CourseClassificationView> classifications = Map.of(
                "MAT1001", EvaluatorTestSupport.classification(CourseType.ACADEMIC_FOUNDATION, "수학"),
                "MAT1002", EvaluatorTestSupport.classification(CourseType.ACADEMIC_FOUNDATION, "수학"),
                "MAT1003", EvaluatorTestSupport.classification(CourseType.ACADEMIC_FOUNDATION, "수학"),
                "PHY1001", EvaluatorTestSupport.classification(CourseType.ACADEMIC_FOUNDATION, "과학"),
                "CSE1001", EvaluatorTestSupport.classification(CourseType.FIRST_MAJOR));
        return EvaluationContext.secondary(transcript, classifications, "복수1");
    }
}
