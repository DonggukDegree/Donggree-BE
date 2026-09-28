package com.donggree.graduation.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.domain.EvaluationContext;
import com.donggree.graduation.internal.domain.RuleResult;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class AcademicFoundationCreditDisplayTest {

    private final GraduationReportAssembler assembler = new GraduationReportAssembler();

    static Stream<Arguments> targets() {
        return Stream.of(
                Arguments.of(
                        "같은 범위",
                        List.of(
                                "{\"areaNames\":[\"수학\"],\"minCredits\":6}",
                                "{\"areaNames\":[\"수학\"],\"minCredits\":9}"),
                        9),
                Arguments.of(
                        "영역 순서와 중복 무관",
                        List.of(
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}",
                                "{\"areaNames\":[\"과학\",\"수학\",\"수학\"],\"minCredits\":24}"),
                        24),
                Arguments.of(
                        "수학과학을 포함한 수학과학전산학",
                        List.of(
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}",
                                "{\"areaNames\":[\"수학\",\"과학\",\"전산학\"],\"minCredits\":30}"),
                        30),
                Arguments.of(
                        "안쪽 요건이 더 큼",
                        List.of(
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":30}",
                                "{\"areaNames\":[\"수학\",\"과학\",\"전산학\"],\"minCredits\":21}"),
                        30),
                Arguments.of(
                        "분리된 기본소양 추가",
                        List.of(
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}",
                                "{\"areaNames\":[\"수학\",\"과학\",\"전산학\"],\"minCredits\":30}",
                                "{\"areaNames\":[\"기본소양\"],\"minCredits\":6}"),
                        36),
                Arguments.of(
                        "부모보다 자식 합계가 큼",
                        List.of(
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":12}",
                                "{\"areaNames\":[\"수학\"],\"minCredits\":9}",
                                "{\"areaNames\":[\"과학\"],\"minCredits\":9}"),
                        18),
                Arguments.of(
                        "여러 단계 포함",
                        List.of(
                                "{\"minCredits\":15}",
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":12}",
                                "{\"areaNames\":[\"수학\"],\"minCredits\":9}",
                                "{\"areaNames\":[\"과학\"],\"minCredits\":9}",
                                "{\"areaNames\":[\"기본소양\"],\"minCredits\":6}"),
                        24),
                Arguments.of(
                        "전체 목표가 더 큼",
                        List.of(
                                "{\"minCredits\":36}",
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}",
                                "{\"areaNames\":[\"기본소양\"],\"minCredits\":6}"),
                        36),
                Arguments.of(
                        "같은 영역 다른 학수번호",
                        List.of(
                                "{\"areaNames\":[\"수학\"],\"courseCodes\":[\"A\"],\"minCredits\":3}",
                                "{\"areaNames\":[\"수학\"],\"courseCodes\":[\"B\"],\"minCredits\":3}"),
                        6),
                Arguments.of(
                        "소분류를 전체로 오인하지 않음",
                        List.of(
                                "{\"areaNames\":[\"기본소양\"],\"minCredits\":6}",
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}",
                                "{\"subCategories\":[\"실험\"],\"minCredits\":4}"),
                        31),
                Arguments.of(
                        "영역 안의 소분류는 중복 합산하지 않음",
                        List.of(
                                "{\"areaNames\":[\"과학\"],\"minCredits\":12}",
                                "{\"areaNames\":[\"과학\"],\"subCategories\":[\"실험\"],\"minCredits\":4}"),
                        12),
                Arguments.of(
                        "다른 소분류",
                        List.of(
                                "{\"areaNames\":[\"과학\"],\"subCategories\":[\"실험\"],\"minCredits\":4}",
                                "{\"areaNames\":[\"과학\"],\"subCategories\":[\"개론\"],\"minCredits\":3}"),
                        7),
                Arguments.of(
                        "PDF 영역 제한",
                        List.of(
                                "{\"pdfAreaNames\":[\"M\"],\"minCredits\":6}",
                                "{\"pdfAreaNames\":[\"S\"],\"minCredits\":3}"),
                        9),
                Arguments.of(
                        "PDF 이수구분 제한",
                        List.of(
                                "{\"pdfCourseTypeNames\":[\"학기\"],\"minCredits\":6}",
                                "{\"pdfCourseTypeNames\":[\"공교\"],\"minCredits\":3}"),
                        9),
                Arguments.of(
                        "과목 접두어 포함 관계",
                        List.of(
                                "{\"courseCodes\":[\"DAI*\"],\"minCredits\":6}",
                                "{\"courseCodes\":[\"DAI1001\"],\"minCredits\":3}"),
                        6),
                Arguments.of(
                        "중복 접두어 정규화",
                        List.of(
                                "{\"courseCodes\":[\"DAI*\"],\"minCredits\":6}",
                                "{\"courseCodes\":[\"DAI*\",\"DAI1001\"],\"minCredits\":9}"),
                        9),
                Arguments.of(
                        "추가 조건이 다르면 동일 영역으로 병합 금지",
                        List.of(
                                "{\"areaNames\":[\"과학\"],\"subCategories\":[\"실험\"],\"minCredits\":4}",
                                "{\"areaNames\":[\"과학\"],\"courseCodes\":[\"SCI*\"],\"minCredits\":3}"),
                        7),
                Arguments.of(
                        "부분 교차는 포함 관계로 취급하지 않음",
                        List.of(
                                "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}",
                                "{\"areaNames\":[\"과학\",\"전산학\"],\"minCredits\":24}"),
                        45),
                Arguments.of("과목 수 규칙 제외", List.of("{\"minCount\":1}", "{\"areaNames\":[\"수학\"],\"minCredits\":6}"), 6),
                Arguments.of("규칙 없음", List.of(), 0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("targets")
    void 학문기초_전체_목표는_동일_포함_분리_범위를_구분한다(String name, List<String> configs, int expected) {
        var rules = IntStream.range(0, configs.size())
                .mapToObj(i -> rule(i + 1L, configs.get(i)))
                .toList();
        assertThat(detail(rules, Map.of()).creditStatus().targetCredits())
                .as(name)
                .isEqualTo(expected);
        var reversed = new ArrayList<>(rules);
        Collections.reverse(reversed);
        assertThat(detail(reversed, Map.of()).creditStatus().targetCredits())
                .as(name + " 역순")
                .isEqualTo(expected);
        var transcript = transcript();
        var report = assembler.assembleReport(transcript, rules, rules, Map.of(), context(), false);
        assertThat(report.areaOverviews()).singleElement().satisfies(area -> assertThat(area.remainingCredits())
                .isEqualTo(Math.max(0, expected - 6)));
    }

    @Test
    void 단일_영역_목표도_서로_다른_과목_요건을_합산하고_실제_규칙_결과로_충족_표시한다() {
        var rules = List.of(
                rule(1, "{\"areaNames\":[\"수학\"],\"courseCodes\":[\"A\"],\"minCredits\":3}"),
                rule(2, "{\"areaNames\":[\"수학\"],\"courseCodes\":[\"B\"],\"minCredits\":3}"));
        var result = detail(rules, Map.of(1L, new RuleResult("A", true), 2L, new RuleResult("B", false)));
        assertThat(result.areaDetails()).singleElement().satisfies(area -> {
            assertThat(area.targetCredits()).isEqualTo(6);
            assertThat(area.earnedCredits()).isEqualTo(6);
            assertThat(area.satisfied()).isFalse();
        });
        assertThat(result.unsatisfiedReasons()).containsExactly("규칙2");
    }

    @Test
    void 다중_영역_목표를_개별_카드에_배분하지_않는다() {
        var rules = List.of(
                rule(1, "{\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21}"),
                rule(2, "{\"areaNames\":[\"수학\",\"과학\",\"전산학\"],\"minCredits\":30}"));
        var result = detail(rules, Map.of());
        assertThat(result.creditStatus().targetCredits()).isEqualTo(30);
        assertThat(result.areaDetails()).singleElement().satisfies(area -> {
            assertThat(area.targetCredits()).isZero();
            assertThat(area.earnedCredits()).isEqualTo(6);
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = CourseType.class,
            names = {"COMMON_GENERAL", "FIRST_MAJOR", "SECOND_MAJOR", "LIBERAL_ARTS"})
    void 다른_이수구분의_기존_전체_목표_계산은_변경하지_않는다(CourseType type) {
        var rules = List.of(
                new GraduationRuleView(1L, "MIN_CREDITS", type, "전체15", "{\"minCredits\":15}"),
                new GraduationRuleView(2L, "MIN_CREDITS", type, "영역18", "{\"areaNames\":[\"영역\"],\"minCredits\":18}"));
        var detail = assembler.assembleAreaDetail(type, rules, rules, Map.of(), context(), Map.of(), List.of());
        assertThat(detail.creditStatus().targetCredits()).isEqualTo(15);
    }

    private GraduationRuleView rule(long id, String config) {
        return new GraduationRuleView(id, "MIN_CREDITS", CourseType.ACADEMIC_FOUNDATION, "규칙" + id, config);
    }

    private AreaDetailProjection detail(List<GraduationRuleView> rules, Map<Long, RuleResult> results) {
        return assembler.assembleAreaDetail(
                CourseType.ACADEMIC_FOUNDATION,
                rules,
                rules,
                results,
                context(),
                Map.of("A", new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "수학", null, null)),
                List.of());
    }

    private EvaluationContext context() {
        return EvaluationContext.report(
                transcript(),
                Map.of("A", new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "수학", null, null)));
    }

    private TranscriptView transcript() {
        return new TranscriptView(
                1L,
                1L,
                100L,
                200L,
                null,
                null,
                null,
                2022,
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
                List.of(new CourseRecordView("2022-1", "A", "학기", "수학", "수학 과목", 6, true, false)));
    }
}
