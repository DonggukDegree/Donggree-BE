package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel.CourseItem;
import com.donggree.graduation.internal.application.report.GraduationReportAssembler;
import com.donggree.graduation.internal.domain.evaluator.courses.PrerequisiteEvaluator;
import com.donggree.graduation.internal.domain.evaluator.courses.RequiredCourseEvaluator;
import com.donggree.graduation.internal.domain.evaluator.credits.GpaEvaluator;
import com.donggree.graduation.internal.domain.evaluator.credits.MinCreditsEvaluator;
import com.donggree.graduation.internal.domain.evaluator.credits.TotalCreditsEvaluator;
import com.donggree.graduation.internal.domain.evaluator.qualification.EnglishCourseEvaluator;
import com.donggree.graduation.internal.domain.evaluator.qualification.ThesisEvaluator;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class GraduationQueryTestSupport {
    static final Long MEMBER_ID = 1L;
    static final Long DEPARTMENT_ID = 100L;
    static final int ADMISSION_YEAR = 2023;
    static final Long REQUIREMENT_SET_ID = 10L;

    @Mock
    TranscriptLookupService transcriptLookupService;

    @Mock
    CurriculumLookupService curriculumLookupService;

    GraduationQueryService service;

    @BeforeEach
    void setUp() {
        service = new GraduationQueryService(
                transcriptLookupService,
                curriculumLookupService,
                List.of(
                        new RequiredCourseEvaluator(),
                        new MinCreditsEvaluator(),
                        new EnglishCourseEvaluator(),
                        new ThesisEvaluator(),
                        new GpaEvaluator(),
                        new PrerequisiteEvaluator(),
                        new TotalCreditsEvaluator()),
                new GraduationReportAssembler());
    }

    void givenMinCreditsRule(CourseType courseType, String ruleName, String ruleConfig) {
        GraduationRuleView rule = new GraduationRuleView(2L, "MIN_CREDITS", courseType, ruleName, ruleConfig);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
    }

    void givenTranscript(CourseRecordView... records) {
        givenTranscriptWithMajors(null, null, null, null, records);
    }

    void assertMajorStatus(CourseType type, boolean satisfied, int achievementRate) {
        assertThat(service.getReport(MEMBER_ID).areaOverviews())
                .filteredOn(area -> type.name().equals(area.courseType()))
                .singleElement()
                .satisfies(area -> {
                    assertThat(area.satisfied()).isEqualTo(satisfied);
                    assertThat(area.achievementRate()).isEqualTo(achievementRate);
                });
    }

    void givenTranscriptWithMajors(Long dual1Id, Long dual2Id, Long sub1Id, Long sub2Id, CourseRecordView... records) {
        givenTranscriptOf(false, dual1Id, dual2Id, sub1Id, sub2Id, records);
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
    }

    void givenTranscriptOf(
            boolean engineeringCertified,
            Long dual1Id,
            Long dual2Id,
            Long sub1Id,
            Long sub2Id,
            CourseRecordView... records) {
        givenTranscriptOf(engineeringCertified, dual1Id, dual2Id, sub1Id, sub2Id, false, records);
    }

    void givenTranscriptOf(
            boolean engineeringCertified,
            Long dual1Id,
            Long dual2Id,
            Long sub1Id,
            Long sub2Id,
            boolean transfer,
            CourseRecordView... records) {
        givenTranscriptOf(engineeringCertified, dual1Id, dual2Id, sub1Id, sub2Id, transfer, false, false, records);
    }

    void givenTranscriptOf(
            boolean engineeringCertified,
            Long dual1Id,
            Long dual2Id,
            Long sub1Id,
            Long sub2Id,
            boolean transfer,
            boolean primaryThesisPassed,
            boolean secondaryThesisPassed,
            CourseRecordView... records) {
        TranscriptView transcript = new TranscriptView(
                1L,
                MEMBER_ID,
                DEPARTMENT_ID,
                dual1Id,
                dual2Id,
                sub1Id,
                sub2Id,
                ADMISSION_YEAR,
                "단일",
                engineeringCertified,
                3,
                BigDecimal.valueOf(4.0),
                "S1",
                false,
                null,
                null,
                primaryThesisPassed,
                secondaryThesisPassed,
                transfer,
                false,
                false,
                List.of(records));
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
    }

    void givenRequiredRule(String courseCode, String ruleName) {
        GraduationRuleView rule = new GraduationRuleView(
                1L, "REQUIRED_COURSE", CourseType.FIRST_MAJOR, ruleName, "{\"courseCodes\":[\"" + courseCode + "\"]}");
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
    }

    void givenClassification(String courseCode, CourseType courseType, String areaName) {
        given(curriculumLookupService.findCourseClassifications(anyList(), eq(ADMISSION_YEAR)))
                .willReturn(Map.of(courseCode, new CourseClassificationView(courseType, areaName, null, null)));
    }

    void givenNoClassification() {
        given(curriculumLookupService.findCourseClassifications(anyList(), eq(ADMISSION_YEAR)))
                .willReturn(Map.of());
    }

    static CourseRecordView passed(String code, String name, int credits, String courseTypeName, String pdfAreaName) {
        return new CourseRecordView("2023-1", code, courseTypeName, pdfAreaName, name, credits, true, false);
    }

    static List<CourseItem> items(AreaDetailReadModel response) {
        return response.areaDetails().stream()
                .flatMap(section -> section.items().stream())
                .toList();
    }
}
