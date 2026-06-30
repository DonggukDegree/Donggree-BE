package com.donggree.graduation.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.graduation.internal.domain.evaluator.RequiredCourseEvaluator;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse.CourseItem;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraduationReportServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DEPARTMENT_ID = 100L;
    private static final int ADMISSION_YEAR = 2023;
    private static final Long REQUIREMENT_SET_ID = 10L;

    @Mock
    private TranscriptLookupService transcriptLookupService;

    @Mock
    private CurriculumLookupService curriculumLookupService;

    private GraduationReportService service;

    @BeforeEach
    void setUp() {
        service = new GraduationReportService(
                transcriptLookupService, curriculumLookupService, List.of(new RequiredCourseEvaluator()));
    }

    // --- inferClassification 매핑 ---

    @Test
    void 이수구분_전공은_제1전공으로_추론된다() {
        assertThat(GraduationReportService.inferClassification("전공").courseType())
                .isEqualTo(CourseType.FIRST_MAJOR);
    }

    @Test
    void 이수구분_전필도_제1전공으로_추론된다() {
        assertThat(GraduationReportService.inferClassification("전필").courseType())
                .isEqualTo(CourseType.FIRST_MAJOR);
    }

    @Test
    void 알_수_없는_이수구분은_courseType이_null로_추론된다() {
        assertThat(GraduationReportService.inferClassification("알수없음").courseType())
                .isNull();
    }

    @Test
    void 이수구분이_null이면_courseType이_null로_추론된다() {
        assertThat(GraduationReportService.inferClassification(null).courseType())
                .isNull();
    }

    // --- 필수 과목을 다른 이수구분으로 충족한 경우 ---

    /**
     * 제1전공 필수 규칙(CSE3001)을 학생이 공통교양 "운영체제특론"으로 이수한 상황.
     * 충족된 필수는 학생이 실제 이수한 공통교양 탭에 실제 과목명으로 SATISFIED 표시되어야 한다.
     */
    @Test
    void 다른_이수구분으로_충족한_필수과목은_실제_이수_탭에_충족으로_표시된다() {
        givenTranscript(passed("CSE3001", "운영체제특론", 3, "공교", null));
        givenRequiredRule("CSE3001", "운영체제는 필수");
        givenClassification("CSE3001", CourseType.COMMON_GENERAL, "기초세계");

        AreaDetailResponse response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(items(response)).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("운영체제특론");
            assertThat(item.status()).isEqualTo("SATISFIED");
            assertThat(item.credit()).isEqualTo(3);
        });
    }

    /**
     * 같은 상황에서 규칙이 속한 제1전공 탭에는 유령 과목으로 중복 표시되지 않아야 한다.
     */
    @Test
    void 다른_이수구분으로_충족한_필수과목은_규칙_탭에_중복_표시되지_않는다() {
        givenTranscript(passed("CSE3001", "운영체제특론", 3, "공교", null));
        givenRequiredRule("CSE3001", "운영체제는 필수");
        givenClassification("CSE3001", CourseType.COMMON_GENERAL, "기초세계");

        AreaDetailResponse response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response)).noneMatch(item -> item.title().contains("운영체제"));
    }

    /**
     * 필수과목(CSE3001)을 이수하지 않은 경우, 규칙이 속한 제1전공 탭에
     * 규칙명·0학점 UNSATISFIED placeholder로 표시되어야 한다.
     */
    @Test
    void 미이수_필수과목은_규칙_탭에_불충족_placeholder로_표시된다() {
        givenTranscript(passed("CSE9999", "자료구조", 3, "전공", "전문"));
        givenRequiredRule("CSE3001", "운영체제는 필수");
        givenNoClassification();

        AreaDetailResponse response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response)).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("운영체제");
            assertThat(item.status()).isEqualTo("UNSATISFIED");
            assertThat(item.credit()).isEqualTo(0);
        });
    }

    // --- 헬퍼 ---

    private void givenTranscript(CourseRecordView... records) {
        TranscriptView transcript = new TranscriptView(
                1L,
                MEMBER_ID,
                DEPARTMENT_ID,
                ADMISSION_YEAR,
                "단일",
                3,
                BigDecimal.valueOf(4.0),
                "S1",
                false,
                null,
                false,
                List.of(records));
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR))
                .willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)));
    }

    private void givenRequiredRule(String courseCode, String ruleName) {
        GraduationRuleView rule = new GraduationRuleView(
                1L, "REQUIRED_COURSE", CourseType.FIRST_MAJOR, ruleName, "{\"courseCodes\":[\"" + courseCode + "\"]}");
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
    }

    private void givenClassification(String courseCode, CourseType courseType, String areaName) {
        given(curriculumLookupService.findCourseClassifications(anyList(), eq(ADMISSION_YEAR)))
                .willReturn(Map.of(courseCode, new CourseClassificationView(courseType, areaName, null, null)));
    }

    private void givenNoClassification() {
        given(curriculumLookupService.findCourseClassifications(anyList(), eq(ADMISSION_YEAR)))
                .willReturn(Map.of());
    }

    private static CourseRecordView passed(
            String code, String name, int credits, String courseTypeName, String pdfAreaName) {
        return new CourseRecordView("2023-1", code, courseTypeName, pdfAreaName, name, credits, true, false);
    }

    private static List<CourseItem> items(AreaDetailResponse response) {
        return response.areaDetails().stream()
                .flatMap(section -> section.items().stream())
                .toList();
    }
}
