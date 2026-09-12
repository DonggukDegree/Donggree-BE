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
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection.CourseItem;
import com.donggree.graduation.internal.domain.evaluator.MinCreditsEvaluator;
import com.donggree.graduation.internal.domain.evaluator.RequiredCourseEvaluator;
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
class GraduationQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DEPARTMENT_ID = 100L;
    private static final int ADMISSION_YEAR = 2023;
    private static final Long REQUIREMENT_SET_ID = 10L;

    @Mock
    private TranscriptLookupService transcriptLookupService;

    @Mock
    private CurriculumLookupService curriculumLookupService;

    private GraduationQueryService service;

    @BeforeEach
    void setUp() {
        service = new GraduationQueryService(
                transcriptLookupService,
                curriculumLookupService,
                List.of(new RequiredCourseEvaluator(), new MinCreditsEvaluator()),
                new GraduationReportAssembler());
    }

    // --- inferClassification 매핑 ---

    @Test
    void 이수구분_전공은_제1전공으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("전공").courseType())
                .isEqualTo(CourseType.FIRST_MAJOR);
    }

    @Test
    void 이수구분_전필도_제1전공으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("전필").courseType())
                .isEqualTo(CourseType.FIRST_MAJOR);
    }

    @Test
    void 알_수_없는_이수구분은_courseType이_null로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("알수없음").courseType())
                .isNull();
    }

    @Test
    void 이수구분이_null이면_courseType이_null로_추론된다() {
        assertThat(GraduationQueryService.inferClassification(null).courseType())
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

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

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

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

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

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response)).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("운영체제");
            assertThat(item.status()).isEqualTo("UNSATISFIED");
            assertThat(item.credit()).isEqualTo(0);
        });
    }

    // --- 최소 이수량 규칙(MIN_CREDITS)의 목표학점 표시 ---

    /**
     * 여러 영역을 합산해 판정하는 규칙(21세기시민·지역연구·미래위험사회와안전 중 2학점)은
     * 목표학점을 어느 한 영역에 귀속시킬 수 없어 섹션에 표시되지 않는다.
     * 대신 미충족 시 unsatisfiedReasons에 규칙명으로 노출되어야 한다.
     */
    @Test
    void 다중_영역_규칙_미충족은_미충족사유로_노출된다() {
        givenTranscript(passed("RGC1075", "동남아지역연구", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"21세기시민\",\"지역연구\",\"미래위험사회와안전\"],"
                        + "\"minCredits\":2}");
        givenClassification("RGC1075", CourseType.COMMON_GENERAL, "지역연구");

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.unsatisfiedReasons()).containsExactly("21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.");
    }

    /** 여러 영역을 합산하는 규칙이 있어도 화면은 영역별로 그대로 나뉘고, 섹션 targetCredits는 0이다. */
    @Test
    void 다중_영역_규칙일_때_개별_영역_섹션의_목표학점은_0이다() {
        givenTranscript(passed("RGC1075", "동남아지역연구", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"21세기시민\",\"지역연구\",\"미래위험사회와안전\"],"
                        + "\"minCredits\":2}");
        givenClassification("RGC1075", CourseType.COMMON_GENERAL, "지역연구");

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.areaDetails())
                .filteredOn(section -> section.areaName().equals("지역연구"))
                .singleElement()
                .satisfies(section -> assertThat(section.targetCredits()).isZero());
    }

    /** 단일 영역 규칙은 목표학점을 그 영역에 귀속시킬 수 있으므로 섹션 targetCredits로 표현된다. */
    @Test
    void 단일_영역_규칙의_목표학점은_섹션에_그대로_표시된다() {
        givenTranscript(passed("RGC1090", "리더십과봉사", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "리더십을 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"리더십\"],\"minCredits\":2}");
        givenClassification("RGC1090", CourseType.COMMON_GENERAL, "리더십");

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.areaDetails())
                .filteredOn(section -> section.areaName().equals("리더십"))
                .singleElement()
                .satisfies(section -> {
                    assertThat(section.targetCredits()).isEqualTo(2);
                    assertThat(section.satisfied()).isFalse();
                });
    }

    /** 영역 제한이 없는 규칙(areaNames 없음)은 이수구분 전체 목표학점이 된다. */
    @Test
    void 이수구분_전체_규칙은_creditStatus의_목표학점이_된다() {
        givenTranscript(passed("RGC0003", "불교와인간", 2, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "공통교양을 17학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"minCredits\":17}");
        givenClassification("RGC0003", CourseType.COMMON_GENERAL, "자아성찰");

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.creditStatus().targetCredits()).isEqualTo(17);
        assertThat(response.creditStatus().earnedCredits()).isEqualTo(2);
        assertThat(response.creditStatus().remainingCredits()).isEqualTo(15);
    }

    /** minCredits가 없는 규칙(ex. 실험 과목 필수 선택 — minCount만 사용)은 목표학점 계산에 반영되지 않는다. */
    @Test
    void minCredits가_없는_규칙은_목표학점에_반영되지_않는다() {
        givenTranscript(passed("PRI4002", "일반물리학및실험1", 3, "학기", null));
        givenMinCreditsRule(
                CourseType.ACADEMIC_FOUNDATION,
                "실험 교과목 중 1과목을 필수 선택해야 합니다.",
                "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"subCategories\":[\"실험\"],\"minCount\":1}");
        givenClassification("PRI4002", CourseType.ACADEMIC_FOUNDATION, "과학");

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);

        assertThat(response.creditStatus().targetCredits()).isZero();
    }

    // --- 헬퍼 ---

    // --- 미지원 전공 안내 ---

    @Test
    void 복수전공도_부전공도_없으면_미지원_안내가_꺼진다() {
        givenTranscript();
        givenMinCreditsRule(CourseType.FIRST_MAJOR, "전공 60학점", "{\"minCredits\":60}");
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isFalse();
    }

    @Test
    void 복수전공이_있으면_미지원_안내가_켜진다() {
        givenTranscriptWithMajors(200L, null, null, null);
        givenMinCreditsRule(CourseType.FIRST_MAJOR, "전공 60학점", "{\"minCredits\":60}");
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    @Test
    void 부전공만_있어도_미지원_안내가_켜진다() {
        givenTranscriptWithMajors(null, null, 300L, null);
        givenMinCreditsRule(CourseType.FIRST_MAJOR, "전공 60학점", "{\"minCredits\":60}");
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    private void givenMinCreditsRule(CourseType courseType, String ruleName, String ruleConfig) {
        GraduationRuleView rule = new GraduationRuleView(2L, "MIN_CREDITS", courseType, ruleName, ruleConfig);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
    }

    private void givenTranscript(CourseRecordView... records) {
        givenTranscriptWithMajors(null, null, null, null, records);
    }

    private void givenTranscriptWithMajors(
            Long dual1Id, Long dual2Id, Long sub1Id, Long sub2Id, CourseRecordView... records) {
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
                3,
                BigDecimal.valueOf(4.0),
                "S1",
                false,
                null,
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

    private static List<CourseItem> items(AreaDetailProjection response) {
        return response.areaDetails().stream()
                .flatMap(section -> section.items().stream())
                .toList();
    }
}
