package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GraduationEnglishCourseReportTest extends GraduationQueryTestSupport {

    @ParameterizedTest
    @CsvSource({"0,false", "3,false", "3,true", "4,false", "4,true"})
    void 전체_영어강의_요건은_두_세트에서_평가해도_요약에_한_번만_표시한다(int courseCount, boolean completedEnglishResult) {
        var records = java.util.stream.IntStream.range(0, courseCount)
                .mapToObj(index -> passed("ENG" + index, "<영어>과목" + index, 3, index % 2 == 0 ? "전공" : "복수1", null))
                .toList();
        var transcript = new TranscriptView(
                1L,
                MEMBER_ID,
                DEPARTMENT_ID,
                200L,
                null,
                null,
                null,
                ADMISSION_YEAR,
                "학사과정",
                false,
                courseCount * 3,
                BigDecimal.valueOf(4.0),
                "S1",
                true,
                completedEnglishResult,
                null,
                false,
                false,
                false,
                false,
                false,
                records);
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        String reason = "영어강의를 총 4개 과목 이상 수강해야 합니다.";
        // 서로 다른 규칙 ID여도 문구는 한 번만 표시하며, PDF 충족 표시는 주전공에만 적용한다.
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "ENGLISH_COURSE",
                        null,
                        reason,
                        "{\"minCount\":4,\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}")));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(new GraduationRuleView(
                        2L,
                        "ENGLISH_COURSE",
                        null,
                        reason,
                        "{\"minCount\":4,\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);
        boolean allPassed = courseCount >= 4;
        assertThat(report.summary().graduated()).isEqualTo(allPassed);
        assertThat(report.summary().achievementRate()).isEqualTo(allPassed ? 100 : completedEnglishResult ? 50 : 0);
        assertThat(report.summary().unsatisfiedReasons())
                .containsExactlyElementsOf(allPassed ? List.of() : List.of(reason));
        for (CourseType type : CourseType.values()) {
            assertThat(service.getAreaDetail(MEMBER_ID, type).unsatisfiedReasons())
                    .isEmpty();
        }
        assertThat(report.areaOverviews()).allSatisfy(area -> {
            assertThat(area.satisfied()).isTrue();
            assertThat(area.achievementRate()).isEqualTo(100);
        });
        var preview = service.preview(transcript);
        assertThat(preview.report()).isEqualTo(report);
        preview.details()
                .forEach((type, detail) -> assertThat(detail).isEqualTo(service.getAreaDetail(MEMBER_ID, type)));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void 여러_역할을_선택해도_주전공의_이수구분_없는_시험은_요약에만_표시한다(boolean dualPrimary) {
        givenTranscriptWithMajors(dualPrimary ? 200L : null, null, null, null);
        if (dualPrimary) {
            willReturn(Optional.empty())
                    .given(curriculumLookupService)
                    .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        }
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "THESIS",
                        null,
                        "졸업시험 합격",
                        "{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);
        assertThat(report.summary().unsatisfiedReasons()).containsExactly("졸업시험 합격");
        assertThat(report.areaOverviews()).isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
    }
}
