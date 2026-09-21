package com.donggree.graduation.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection.CourseItem;
import com.donggree.graduation.internal.domain.evaluator.EnglishCourseEvaluator;
import com.donggree.graduation.internal.domain.evaluator.GpaEvaluator;
import com.donggree.graduation.internal.domain.evaluator.MinCreditsEvaluator;
import com.donggree.graduation.internal.domain.evaluator.PrerequisiteEvaluator;
import com.donggree.graduation.internal.domain.evaluator.RequiredCourseEvaluator;
import com.donggree.graduation.internal.domain.evaluator.ThesisEvaluator;
import com.donggree.graduation.internal.domain.evaluator.TotalCreditsEvaluator;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    void 이수구분_복수는_제2전공으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("복수1").courseType())
                .isEqualTo(CourseType.SECOND_MAJOR);
        assertThat(GraduationQueryService.inferClassification("복수2").courseType())
                .isEqualTo(CourseType.SECOND_MAJOR);
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
    void 단일전공의_교양_규칙_사유는_제1전공에_표시하고_학점은_교양에_유지한다() {
        givenTranscript(passed("RGC1075", "동남아지역연구", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"21세기시민\",\"지역연구\",\"미래위험사회와안전\"],"
                        + "\"minCredits\":2}");
        givenClassification("RGC1075", CourseType.COMMON_GENERAL, "지역연구");

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        var primary = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);
        assertThat(response.unsatisfiedReasons()).isEmpty();
        assertThat(primary.unsatisfiedReasons()).containsExactly("21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.");
        assertThat(response.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(1, 2, 1));
        assertThat(primary.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(0, 0, 0));
        assertThat(items(primary)).isEmpty();
        assertMajorStatus(CourseType.FIRST_MAJOR, false, 0);
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
    void 복수전공은_평가하므로_미지원_안내가_꺼진다() {
        givenTranscriptWithMajors(200L, null, null, null, passed("DAI1001", "복수전공", 3, "복수1", "전문"));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(new GraduationRuleView(
                        3L,
                        "MIN_CREDITS",
                        CourseType.FIRST_MAJOR,
                        "복수전공 3학점",
                        "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":3,\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isFalse();
    }

    @Test
    void 복수전공_학생은_주전공과_복수전공_규칙을_각각_평가한다() {
        givenTranscriptWithMajors(
                200L,
                null,
                null,
                null,
                passed("CSE1001", "주전공", 3, "전공", "전문"),
                passed("DAI1001", "복수전공", 3, "복수1", "전문"));
        GraduationRuleView singleOnly = new GraduationRuleView(
                1L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "단일전공 72학점",
                "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":72,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}");
        GraduationRuleView dualPrimary = new GraduationRuleView(
                2L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "복수전공자 주전공 3학점",
                "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":3,\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}");
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(singleOnly, dualPrimary));
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        GraduationRuleView secondary = new GraduationRuleView(
                3L,
                "MIN_CREDITS",
                CourseType.FIRST_MAJOR,
                "복수전공 3학점",
                "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":3,\"applicableMajorRoles\":[\"SECONDARY\"]}");
        given(curriculumLookupService.findGraduationRules(20L)).willReturn(List.of(secondary));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);

        assertThat(report.summary().achievementRate()).isEqualTo(100);
        assertThat(report.areaOverviews())
                .anySatisfy(area -> {
                    assertThat(area.courseType()).isEqualTo("FIRST_MAJOR");
                    assertThat(area.satisfied()).isTrue();
                })
                .anySatisfy(area -> {
                    assertThat(area.courseType()).isEqualTo("SECOND_MAJOR");
                    assertThat(area.satisfied()).isTrue();
                    assertThat(area.remainingCredits()).isZero();
                });
    }

    @Test
    void 복수전공_학과의_적용_세트가_없어도_주전공_리포트와_경고를_반환한다() {
        givenTranscriptWithMajors(200L, null, null, null);
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.empty());
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void 주전공과_복수전공_시험을_독립_판정하고_총학점은_주전공에서만_검사한다(boolean primaryPassed, boolean secondaryPassed) {
        givenTranscriptOf(false, 200L, null, null, null, false, primaryPassed, secondaryPassed);
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)));
        // 기존 옵션 없는 THESIS도 복수전공자의 주전공 요건으로 유지한다.
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        new GraduationRuleView(1L, "THESIS", null, "졸업시험 합격", "{}"),
                        new GraduationRuleView(2L, "TOTAL_CREDITS", null, "총학점 3 이상", "{\"minCredits\":3}")));
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        // 같은 이름·ID라도 두 전공의 판정과 각 탭의 사유가 덮어써지지 않아야 한다.
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L, "THESIS", null, "졸업시험 합격", "{\"applicableMajorRoles\":[\"SECONDARY\"]}"),
                        new GraduationRuleView(3L, "TOTAL_CREDITS", null, "B학과 총학점 140 이상", "{\"minCredits\":140}"),
                        new GraduationRuleView(4L, "THESIS", null, "B학과 주전공 전용 시험", "{}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);

        assertThat(report.summary().graduated()).isEqualTo(primaryPassed && secondaryPassed);
        assertThat(report.summary().targetCredits()).isEqualTo(3);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        var primary = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);
        var secondary = service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR);
        assertThat(primary.unsatisfiedReasons())
                .containsExactlyElementsOf(primaryPassed ? List.of() : List.of("졸업시험 합격"));
        assertThat(secondary.unsatisfiedReasons())
                .containsExactlyElementsOf(secondaryPassed ? List.of() : List.of("[복수전공] 졸업시험 합격"));
        // 이수한 전공 과목이 없어도 시험 요건을 확인할 탭과 판정 카드가 존재해야 한다.
        assertMajorStatus(CourseType.FIRST_MAJOR, primaryPassed, primaryPassed ? 100 : 0);
        assertMajorStatus(CourseType.SECOND_MAJOR, secondaryPassed, secondaryPassed ? 100 : 0);
        assertThat(primary.areaDetails()).isEmpty();
        assertThat(secondary.areaDetails()).isEmpty();
        assertThat(secondary.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(0, 0, 0));
        assertThat(report.hasUnsupportedMajor()).isFalse();
    }

    @Test
    void 두_번째_복수전공은_판정하지_않고_정확도_경고를_반환한다() {
        givenTranscriptWithMajors(null, 200L, null, null);
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void 복수전공_학문기초의_사유는_전공별로_분리하고_과목과_학점은_학문기초에_유지한다(boolean foundationPassed) {
        givenTranscriptWithMajors(
                200L,
                null,
                null,
                null,
                new CourseRecordView("2023-1", "MAT1001", "학기", null, "미적분학", 3, foundationPassed, false));
        String sharedRoles = "\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]";
        var foundationMin = new GraduationRuleView(
                1L,
                "MIN_CREDITS",
                CourseType.ACADEMIC_FOUNDATION,
                "학문기초 3학점",
                "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"minCredits\":3," + sharedRoles + "}");
        var foundationRequired = new GraduationRuleView(
                2L,
                "REQUIRED_COURSE",
                CourseType.ACADEMIC_FOUNDATION,
                "미적분학은 필수",
                "{\"courseCodes\":[\"MAT1001\"]," + sharedRoles + "}");
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        foundationMin,
                        foundationRequired,
                        new GraduationRuleView(3L, "TOTAL_CREDITS", null, "총학점 3 이상", "{\"minCredits\":3}")));
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(
                        foundationMin,
                        foundationRequired,
                        new GraduationRuleView(4L, "TOTAL_CREDITS", null, "B학과 총학점 140", "{\"minCredits\":140}"),
                        new GraduationRuleView(
                                5L,
                                "MIN_CREDITS",
                                CourseType.ACADEMIC_FOUNDATION,
                                "선택하지 않은 100학점",
                                "{\"minCredits\":100,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\"]}"),
                        new GraduationRuleView(
                                6L,
                                "REQUIRED_COURSE",
                                CourseType.ACADEMIC_FOUNDATION,
                                "옵션 없는 필수",
                                "{\"courseCodes\":[\"MISSING\"]}")));
        givenClassification("MAT1001", CourseType.ACADEMIC_FOUNDATION, "수학");

        var report = service.getReport(MEMBER_ID);
        var detail = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);
        var primary = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);
        var secondary = service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR);

        assertThat(report.summary().graduated()).isEqualTo(foundationPassed);
        assertThat(report.summary().achievementRate()).isEqualTo(foundationPassed ? 100 : 20);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(report.hasUnsupportedMajor()).isFalse();
        assertThat(detail.unsatisfiedReasons()).isEmpty();
        assertThat(primary.unsatisfiedReasons())
                .containsExactlyElementsOf(foundationPassed ? List.of() : List.of("학문기초 3학점", "미적분학은 필수"));
        assertThat(secondary.unsatisfiedReasons())
                .containsExactlyElementsOf(
                        foundationPassed ? List.of() : List.of("[복수전공] 학문기초 3학점", "[복수전공] 미적분학은 필수"));
        assertMajorStatus(CourseType.FIRST_MAJOR, foundationPassed, foundationPassed ? 100 : 0);
        assertMajorStatus(CourseType.SECOND_MAJOR, foundationPassed, foundationPassed ? 100 : 0);
        assertThat(detail.creditStatus())
                .isEqualTo(
                        new AreaDetailProjection.CreditStatus(foundationPassed ? 3 : 0, 3, foundationPassed ? 0 : 3));
        assertThat(primary.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(0, 0, 0));
        assertThat(secondary.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(0, 0, 0));
        assertThat(items(primary)).isEmpty();
        assertThat(items(secondary)).isEmpty();
        if (foundationPassed) {
            assertThat(detail.unsatisfiedReasons()).isEmpty();
            assertThat(items(detail)).singleElement().satisfies(item -> {
                assertThat(item.title()).isEqualTo("미적분학");
                assertThat(item.status()).isEqualTo("SATISFIED");
            });
        }
    }

    @Test
    void 편입생은_리포트를_반환하면서_정확도_경고를_표시한다() {
        givenTranscriptOf(false, null, null, null, null, true);
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    @Test
    void 복수전공_학과에만_있는_학문기초_필수도_실제_이수_탭에서_충족_표시한다() {
        givenTranscriptWithMajors(200L, null, null, null, passed("MAT1001", "미적분학", 3, "일교", null));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "REQUIRED_COURSE",
                        CourseType.ACADEMIC_FOUNDATION,
                        "미적분학은 필수",
                        "{\"courseCodes\":[\"MAT1001\"],\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        givenNoClassification();

        var detail = service.getAreaDetail(MEMBER_ID, CourseType.LIBERAL_ARTS);
        var foundation = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);

        assertThat(items(detail)).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("미적분학");
            assertThat(item.status()).isEqualTo("SATISFIED");
        });
        assertThat(items(foundation)).isEmpty();
        assertThat(foundation.unsatisfiedReasons()).isEmpty();
    }

    @Test
    void 두_학과의_동일_학문기초_영역_목표는_더_큰_기준으로_표시한다() {
        givenTranscriptWithMajors(200L, null, null, null, passed("MAT1001", "미적분학", 3, "학기", null));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(
                        List.of(
                                new GraduationRuleView(
                                        1L,
                                        "MIN_CREDITS",
                                        CourseType.ACADEMIC_FOUNDATION,
                                        "수학 6학점",
                                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"수학\"],\"minCredits\":6,\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}")));
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(
                        List.of(
                                new GraduationRuleView(
                                        2L,
                                        "MIN_CREDITS",
                                        CourseType.ACADEMIC_FOUNDATION,
                                        "수학 3학점",
                                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"수학\"],\"minCredits\":3,\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        givenClassification("MAT1001", CourseType.ACADEMIC_FOUNDATION, "수학");

        var detail = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);

        assertThat(detail.creditStatus().targetCredits()).isEqualTo(6);
        assertThat(detail.creditStatus().remainingCredits()).isEqualTo(3);
        assertThat(detail.unsatisfiedReasons()).isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .containsExactly("수학 6학점");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertMajorStatus(CourseType.FIRST_MAJOR, false, 0);
        assertMajorStatus(CourseType.SECOND_MAJOR, true, 100);
        assertThat(detail.areaDetails()).singleElement().satisfies(area -> {
            assertThat(area.targetCredits()).isEqualTo(6);
            assertThat(area.satisfied()).isFalse();
        });
    }

    @Test
    void 복수전공_필수과목을_주전공에서_이수해도_주전공_화면의_필수_충족으로_표시하지_않는다() {
        givenTranscriptWithMajors(200L, null, null, null, passed("DAI1001", "인공지능", 3, "전공", "전문"));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        GraduationRuleView secondaryRequired = new GraduationRuleView(
                3L,
                "REQUIRED_COURSE",
                CourseType.FIRST_MAJOR,
                "인공지능은 필수",
                "{\"courseCodes\":[\"DAI1001\"],\"applicableMajorRoles\":[\"SECONDARY\"]}");
        given(curriculumLookupService.findGraduationRules(20L)).willReturn(List.of(secondaryRequired));
        givenNoClassification();

        AreaDetailProjection response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response))
                .filteredOn(item -> item.title().equals("인공지능"))
                .singleElement()
                .satisfies(item -> assertThat(item.status()).isEqualTo("OPTIONAL"));
    }

    @Test
    void 부전공만_있어도_미지원_안내가_켜진다() {
        givenTranscriptWithMajors(null, null, 300L, null);
        givenMinCreditsRule(CourseType.FIRST_MAJOR, "전공 60학점", "{\"minCredits\":60}");
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    // --- 과정(일반/심화)별 요건 세트 선택 ---

    @Test
    void 심화과정_학생은_공학인증심화대상_여부를_넘겨_세트를_조회한다() {
        givenTranscriptOf(true, null, null, null, null);
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, true))
                .willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());

        assertThat(service.getReport(MEMBER_ID)).isNotNull();
    }

    @Test
    void 심화과정_학생에게_맞는_세트가_없으면_미지원_학과와_동일하게_리포트_생성이_실패한다() {
        // 일반과정 세트만 등록된 학과. 그 요건으로 대신 판정하지 않고 실패시킨다.
        givenTranscriptOf(true, null, null, null, null);
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, true))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReport(MEMBER_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
    }

    private void givenMinCreditsRule(CourseType courseType, String ruleName, String ruleConfig) {
        GraduationRuleView rule = new GraduationRuleView(2L, "MIN_CREDITS", courseType, ruleName, ruleConfig);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void 두_학과의_영어강의를_역할별로_독립_판정하고_선택하지_않은_B학과_규칙은_제외한다(boolean primaryPassed, boolean secondaryPassed) {
        var records = List.of(
                new CourseRecordView("2023-1", "P1", "전공", null, "<영어>주전공1", 3, primaryPassed, false),
                new CourseRecordView("2023-1", "P2", "전필", null, "<영어>주전공2", 3, primaryPassed, false),
                new CourseRecordView("2023-1", "B1", "복수1", null, "<영어>복수전공1", 3, secondaryPassed, false),
                new CourseRecordView("2023-1", "B2", "복수1", null, "<영어>복수전공2", 3, secondaryPassed, false));
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
                12,
                BigDecimal.valueOf(4.0),
                "S1",
                true,
                null,
                null,
                false,
                false,
                false,
                records);
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)));
        // 옵션 없는 기존 영어강의는 복수전공자의 주전공에서도 유지한다.
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L,
                                "ENGLISH_COURSE",
                                null,
                                "전공 영어강의 2과목",
                                "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2}"),
                        new GraduationRuleView(
                                2L,
                                "ENGLISH_COURSE",
                                null,
                                "단일전공 전용 요건",
                                "{\"minCount\":99,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}")));
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L,
                                "ENGLISH_COURSE",
                                null,
                                "전공 영어강의 2과목",
                                "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2,\"applicableMajorRoles\":[\"SECONDARY\"]}"),
                        new GraduationRuleView(3L, "ENGLISH_COURSE", null, "B학과 옵션 없는 영어강의", "{\"minCount\":99}"),
                        new GraduationRuleView(4L, "TOTAL_CREDITS", null, "B학과 총학점 140", "{\"minCredits\":140}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);

        assertThat(report.summary().graduated()).isEqualTo(primaryPassed && secondaryPassed);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .containsExactlyElementsOf(primaryPassed ? List.of() : List.of("전공 영어강의 2과목"));
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .containsExactlyElementsOf(secondaryPassed ? List.of() : List.of("[복수전공] 전공 영어강의 2과목"));
        assertMajorStatus(CourseType.FIRST_MAJOR, primaryPassed, primaryPassed ? 100 : 0);
        assertMajorStatus(CourseType.SECOND_MAJOR, secondaryPassed, secondaryPassed ? 100 : 0);
        assertThat(report.hasUnsupportedMajor()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void 여러_역할을_선택한_규칙도_실제_적용된_주전공_탭에만_표시한다(boolean dualPrimary) {
        givenTranscriptWithMajors(dualPrimary ? 200L : null, null, null, null);
        if (dualPrimary) {
            given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                    .willReturn(Optional.empty());
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
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(report.areaOverviews()).extracting(area -> area.courseType()).containsExactly("FIRST_MAJOR");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .containsExactly("졸업시험 합격");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertMajorStatus(CourseType.FIRST_MAJOR, false, 0);
    }

    @Test
    void 역할_옵션이_없는_총학점_평점_선이수_사유는_기존_위치에_유지한다() {
        givenTranscript(passed("EAS2", "EAS2", 3, "공교", null));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        new GraduationRuleView(1L, "TOTAL_CREDITS", null, "총학점 130 이상", "{\"minCredits\":130}"),
                        new GraduationRuleView(2L, "GPA", null, "평점 4.5 이상", "{\"minGpa\":4.5}"),
                        new GraduationRuleView(
                                3L,
                                "PREREQUISITE",
                                CourseType.COMMON_GENERAL,
                                "EAS1 선이수",
                                "{\"targetCourseCodes\":[\"EAS2\"],\"prerequisiteCourseCodes\":[\"EAS1\"]}"),
                        new GraduationRuleView(4L, "THESIS", null, "졸업시험 합격", "{}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);
        assertThat(report.summary().unsatisfiedReasons()).containsExactly("총학점 130 이상", "평점 4.5 이상");
        assertThat(report.summary().achievementRate()).isZero();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL).unsatisfiedReasons())
                .containsExactly("EAS1 선이수");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .containsExactly("졸업시험 합격");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
    }

    @Test
    void 전공_달성률은_역할별_요건으로_계산하고_학점과_과목은_원래_분류로_유지한다() {
        givenTranscriptWithMajors(
                200L,
                null,
                null,
                null,
                passed("P1", "주전공과목", 3, "전공", "전문"),
                passed("B1", "복수전공과목", 3, "복수1", "전문"),
                passed("MAT1", "미적분학", 3, "학기", null));
        String roles = "\"applicableMajorRoles\":[\"DUAL_PRIMARY\",\"SECONDARY\"]";
        var foundation = new GraduationRuleView(
                1L,
                "MIN_CREDITS",
                CourseType.ACADEMIC_FOUNDATION,
                "학문기초 3학점",
                "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"minCredits\":3," + roles + "}");
        var thesis = new GraduationRuleView(2L, "THESIS", null, "졸업시험 합격", "{" + roles + "}");
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        foundation,
                        thesis,
                        new GraduationRuleView(
                                3L,
                                "MIN_CREDITS",
                                CourseType.FIRST_MAJOR,
                                "전공 3학점",
                                "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":3," + roles + "}")));
        given(curriculumLookupService.findActiveRequirementSet(200L, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)));
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(
                        foundation,
                        thesis,
                        new GraduationRuleView(
                                4L,
                                "MIN_CREDITS",
                                CourseType.FIRST_MAJOR,
                                "전공 6학점",
                                "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":6," + roles + "}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);
        var primary = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);
        var secondary = service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR);
        var academic = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);
        assertThat(report.summary().achievementRate()).isEqualTo(50);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertMajorStatus(CourseType.FIRST_MAJOR, false, 66);
        assertMajorStatus(CourseType.SECOND_MAJOR, false, 33);
        assertThat(primary.unsatisfiedReasons()).containsExactly("졸업시험 합격");
        assertThat(secondary.unsatisfiedReasons()).containsExactly("[복수전공] 졸업시험 합격", "전공 6학점");
        assertThat(academic.unsatisfiedReasons()).isEmpty();
        assertThat(primary.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(3, 3, 0));
        assertThat(secondary.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(3, 6, 3));
        assertThat(academic.creditStatus()).isEqualTo(new AreaDetailProjection.CreditStatus(3, 3, 0));
        assertThat(items(primary)).extracting(CourseItem::title).containsExactly("주전공과목");
        assertThat(items(secondary)).extracting(CourseItem::title).containsExactly("복수전공과목");
        assertThat(items(academic)).extracting(CourseItem::title).containsExactly("미적분학");
    }

    private void givenTranscript(CourseRecordView... records) {
        givenTranscriptWithMajors(null, null, null, null, records);
    }

    private void assertMajorStatus(CourseType type, boolean satisfied, int achievementRate) {
        assertThat(service.getReport(MEMBER_ID).areaOverviews())
                .filteredOn(area -> type.name().equals(area.courseType()))
                .singleElement()
                .satisfies(area -> {
                    assertThat(area.satisfied()).isEqualTo(satisfied);
                    assertThat(area.achievementRate()).isEqualTo(achievementRate);
                });
    }

    private void givenTranscriptWithMajors(
            Long dual1Id, Long dual2Id, Long sub1Id, Long sub2Id, CourseRecordView... records) {
        givenTranscriptOf(false, dual1Id, dual2Id, sub1Id, sub2Id, records);
        given(curriculumLookupService.findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false))
                .willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)));
    }

    /** 성적표만 준비한다. 요건 세트 조회 스텁은 호출부에서 과정에 맞춰 따로 건다. */
    private void givenTranscriptOf(
            boolean engineeringCertified,
            Long dual1Id,
            Long dual2Id,
            Long sub1Id,
            Long sub2Id,
            CourseRecordView... records) {
        givenTranscriptOf(engineeringCertified, dual1Id, dual2Id, sub1Id, sub2Id, false, records);
    }

    private void givenTranscriptOf(
            boolean engineeringCertified,
            Long dual1Id,
            Long dual2Id,
            Long sub1Id,
            Long sub2Id,
            boolean transfer,
            CourseRecordView... records) {
        givenTranscriptOf(engineeringCertified, dual1Id, dual2Id, sub1Id, sub2Id, transfer, false, false, records);
    }

    private void givenTranscriptOf(
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
                List.of(records));
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
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
