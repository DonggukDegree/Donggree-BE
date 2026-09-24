package com.donggree.graduation.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.graduation.internal.domain.evaluator.MinCreditsEvaluator;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GraduationAreaDisplayTest {
    private final GraduationReportAssembler assembler = new GraduationReportAssembler();

    @ParameterizedTest
    @CsvSource({"FIRST_MAJOR,전공", "FIRST_MAJOR,전필", "SECOND_MAJOR,복수1", "SECOND_MAJOR,복수2"})
    void 두_전공의_영역_표시명을_통일하고_원문과_학점을_유지한다(CourseType type, String category) {
        var transcript = transcript(List.of(course("CSE0001", category, "기초"), course("CSE0002", category, "전문")));
        var classifications = Map.of(
                "CSE0001", new CourseClassificationView(type, null, null, null),
                "CSE0002", new CourseClassificationView(type, null, null, null));

        var detail = detail(type, transcript, classifications, List.of(), Map.of());

        assertThat(detail.areaDetails())
                .extracting(AreaDetailProjection.AreaSection::areaName)
                .containsExactly("전공기초", "전공전문");
        assertThat(detail.creditStatus().earnedCredits()).isEqualTo(6);
        assertThat(transcript.courseRecords())
                .extracting(CourseRecordView::pdfAreaName)
                .containsExactly("기초", "전문");
        assertThat(classifications.values())
                .allSatisfy(cls -> assertThat(cls.areaName()).isNull());
    }

    @Test
    void 분류정보의_축약명과_정식명을_같은_영역으로_묶고_목표와_필수_안내를_유지한다() {
        var transcript = transcript(List.of(course("CSE0001", "복수1", "기초"), course("CSE0002", "복수1", "기초")));
        var classifications = Map.of(
                "CSE0001", new CourseClassificationView(CourseType.FIRST_MAJOR, "기초", null, null),
                "CSE0002", new CourseClassificationView(CourseType.FIRST_MAJOR, "전공기초", null, null),
                "CSE0003", new CourseClassificationView(CourseType.FIRST_MAJOR, "기초", null, null));
        var minimum = new GraduationRuleView(
                1L, "MIN_CREDITS", CourseType.SECOND_MAJOR, "기초 9학점", "{\"areaNames\":[\"기초\"],\"minCredits\":9}");
        var required = new GraduationRuleView(
                2L, "REQUIRED_COURSE", CourseType.SECOND_MAJOR, "필수과목은 필수", "{\"courseCodes\":[\"CSE0003\"]}");

        var detail = detail(
                CourseType.SECOND_MAJOR,
                transcript,
                classifications,
                List.of(minimum, required),
                Map.of(1L, new RuleResult(minimum.ruleName(), false), 2L, new RuleResult(required.ruleName(), false)));

        assertThat(detail.areaDetails()).singleElement().satisfies(section -> {
            assertThat(section.areaName()).isEqualTo("전공기초");
            assertThat(section.earnedCredits()).isEqualTo(6);
            assertThat(section.targetCredits()).isEqualTo(9);
            assertThat(section.satisfied()).isFalse();
            assertThat(section.items()).hasSize(3).anySatisfy(item -> {
                assertThat(item.title()).isEqualTo("필수과목");
                assertThat(item.status()).isEqualTo("UNSATISFIED");
            });
        });
    }

    @ParameterizedTest
    @CsvSource({"COMMON_GENERAL,공교", "ACADEMIC_FOUNDATION,학기", "LIBERAL_ARTS,일교"})
    void 비전공_탭의_명시적_영역명은_바꾸지_않는다(CourseType type, String category) {
        var transcript = transcript(List.of(course("CSE0001", category, "기초")));
        var detail = detail(
                type,
                transcript,
                Map.of("CSE0001", new CourseClassificationView(type, "기초", null, null)),
                List.of(),
                Map.of());
        assertThat(detail.areaDetails())
                .extracting(AreaDetailProjection.AreaSection::areaName)
                .containsExactly("기초");
    }

    @Test
    void 표시_변환_후에도_전문_원문_선택자의_복수전공_규칙_판정은_유지한다() {
        var transcript = transcript(List.of(course("CSE0001", "복수1", "전문")));
        var classifications = Map.of("CSE0001", new CourseClassificationView(CourseType.FIRST_MAJOR, null, null, null));
        var context = EvaluationContext.secondary(transcript, classifications, "복수1");
        var rule = new GraduationRuleView(
                1L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "전문 3학점",
                "{\"courseType\":\"FIRST_MAJOR\",\"pdfAreaNames\":[\"전문\"],\"minCredits\":3}");
        var before = new MinCreditsEvaluator().evaluate(rule, context);

        var detail = detail(CourseType.SECOND_MAJOR, transcript, classifications, List.of(), Map.of());

        assertThat(detail.areaDetails())
                .extracting(AreaDetailProjection.AreaSection::areaName)
                .containsExactly("전공전문");
        assertThat(new MinCreditsEvaluator().evaluate(rule, context)).isEqualTo(before);
        assertThat(before.satisfied()).isTrue();
    }

    private AreaDetailProjection detail(
            CourseType type,
            TranscriptView transcript,
            Map<String, CourseClassificationView> classifications,
            List<GraduationRuleView> rules,
            Map<Long, RuleResult> results) {
        var context = EvaluationContext.report(transcript, classifications);
        var allClassifications = new HashMap<>(classifications);
        // 실제 조회 서비스와 동일한 복수전공 수강별 분류 보충 경로
        context.getPassedCoursesByType(type)
                .forEach(record -> allClassifications.put(record.courseCode(), context.getClassification(record)));
        return assembler.assembleAreaDetail(type, rules, rules, results, context, allClassifications, List.of());
    }

    private CourseRecordView course(String code, String category, String area) {
        return new CourseRecordView("2023-1", code, category, area, code + " 과목", 3, true, false);
    }

    private TranscriptView transcript(List<CourseRecordView> records) {
        return new TranscriptView(
                1L,
                1L,
                100L,
                200L,
                null,
                null,
                null,
                2023,
                "학사과정",
                false,
                6,
                BigDecimal.valueOf(3.5),
                null,
                false,
                null,
                null,
                false,
                false,
                false,
                true,
                false,
                records);
    }
}
