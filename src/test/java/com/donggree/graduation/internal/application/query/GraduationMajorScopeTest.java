package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class GraduationMajorScopeTest extends GraduationQueryTestSupport {

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
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
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
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
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
        willReturn(Optional.empty())
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    @Test
    void 복수전공_학과_ID를_못_찾아도_단일전공_규칙으로_바꾸지_않는다() {
        TranscriptView transcript = new TranscriptView(
                1L,
                MEMBER_ID,
                DEPARTMENT_ID,
                null,
                null,
                null,
                null,
                ADMISSION_YEAR,
                "단일",
                false,
                3,
                BigDecimal.valueOf(4.0),
                "S1",
                false,
                null,
                null,
                false,
                false,
                false,
                true,
                true,
                List.of(passed("CSE1001", "주전공", 3, "전공", "전문")));
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(
                        List.of(
                                new GraduationRuleView(
                                        1L,
                                        "MIN_CREDITS",
                                        CourseType.FIRST_MAJOR,
                                        "단일전공 72학점",
                                        "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":72,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}"),
                                new GraduationRuleView(
                                        2L,
                                        "MIN_CREDITS",
                                        CourseType.FIRST_MAJOR,
                                        "복수전공자 주전공 3학점",
                                        "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":3,\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);

        assertThat(report.hasUnsupportedMajor()).isTrue();
        assertThat(report.summary().achievementRate()).isEqualTo(100);
        assertThat(report.areaOverviews()).singleElement().satisfies(area -> {
            assertThat(area.courseType()).isEqualTo("FIRST_MAJOR");
            assertThat(area.satisfied()).isTrue();
        });
        // 저장 성적표와 일회성 미리보기에 동일한 역할·경고 적용
        assertThat(service.preview(transcript).report()).isEqualTo(report);
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void 주전공과_복수전공_시험을_독립_판정하고_총학점은_주전공에서만_검사한다(boolean primaryPassed, boolean secondaryPassed) {
        givenTranscriptOf(false, 200L, null, null, null, false, primaryPassed, secondaryPassed);
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
        // 기존 옵션 없는 THESIS도 복수전공자의 주전공 요건으로 유지한다.
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        new GraduationRuleView(1L, "THESIS", null, "졸업시험 합격", "{}"),
                        new GraduationRuleView(2L, "TOTAL_CREDITS", null, "총학점 3 이상", "{\"minCredits\":3}")));
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        // 같은 이름·ID여도 두 전공을 독립 판정하고, 요약의 동일한 문구만 한 번 표시한다.
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L, "THESIS", null, "졸업시험 합격", "{\"applicableMajorRoles\":[\"SECONDARY\"]}"),
                        new GraduationRuleView(3L, "TOTAL_CREDITS", null, "B학과 총학점 140 이상", "{\"minCredits\":140}"),
                        new GraduationRuleView(4L, "THESIS", null, "B학과 주전공 전용 시험", "{}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);

        assertThat(report.summary().graduated()).isEqualTo(primaryPassed && secondaryPassed);
        assertThat(report.summary().achievementRate())
                .isEqualTo((1 + (primaryPassed ? 1 : 0) + (secondaryPassed ? 1 : 0)) * 100 / 3);
        assertThat(report.summary().targetCredits()).isEqualTo(3);
        assertThat(report.summary().unsatisfiedReasons())
                .containsExactlyElementsOf(primaryPassed && secondaryPassed ? List.of() : List.of("졸업시험 합격"));
        var primary = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);
        var secondary = service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR);
        assertThat(primary.unsatisfiedReasons()).isEmpty();
        assertThat(secondary.unsatisfiedReasons()).isEmpty();
        // 이수구분 없는 두 시험은 요약에만 표시하고, 빈 전공 탭을 생성하지 않는다.
        assertThat(report.areaOverviews()).isEmpty();
        assertThat(primary.areaDetails()).isEmpty();
        assertThat(secondary.areaDetails()).isEmpty();
        assertThat(secondary.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(0, 0, 0));
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
    void 두_학과의_학문기초_사유는_학문기초에_중복_없이_표시한다(boolean foundationPassed) {
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
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
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
        assertThat(detail.unsatisfiedReasons())
                .containsExactlyElementsOf(foundationPassed ? List.of() : List.of("학문기초 3학점", "미적분학은 필수"));
        assertThat(primary.unsatisfiedReasons()).isEmpty();
        assertThat(secondary.unsatisfiedReasons()).isEmpty();
        assertThat(report.areaOverviews()).extracting(area -> area.courseType()).containsExactly("ACADEMIC_FOUNDATION");
        assertMajorStatus(CourseType.ACADEMIC_FOUNDATION, foundationPassed, foundationPassed ? 100 : 0);
        assertThat(detail.creditStatus())
                .isEqualTo(new AreaDetailReadModel.CreditStatus(foundationPassed ? 3 : 0, 3, foundationPassed ? 0 : 3));
        assertThat(primary.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(0, 0, 0));
        assertThat(secondary.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(0, 0, 0));
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
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        givenNoClassification();

        assertThat(service.getReport(MEMBER_ID).hasUnsupportedMajor()).isTrue();
    }

    @Test
    void 복수전공_학과에만_있는_학문기초_필수도_실제_이수_탭에서_충족_표시한다() {
        givenTranscriptWithMajors(200L, null, null, null, passed("MAT1001", "미적분학", 3, "일교", null));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
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

    @ParameterizedTest
    @CsvSource({"false,true", "false,false", "true,true", "true,false"})
    void 공통교양_필수과목은_두_세트의_적용_대상에_따라_독립_판정한다(boolean appliesToDualPrimary, boolean writingPassed) {
        givenTranscriptWithMajors(
                200L,
                null,
                null,
                null,
                writingPassed
                        ? new CourseRecordView[] {passed("RGC1001", "글쓰기", 3, "공교", "글")}
                        : new CourseRecordView[0]);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "REQUIRED_COURSE",
                        CourseType.COMMON_GENERAL,
                        "글쓰기는 필수",
                        "{\"courseCodes\":[\"RGC1001\"],\"applicableMajorRoles\":"
                                + (appliesToDualPrimary
                                        ? "[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\"]"
                                        : "[\"SINGLE_PRIMARY\"]")
                                + "}")));
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(
                        List.of(
                                new GraduationRuleView(
                                        2L,
                                        "REQUIRED_COURSE",
                                        CourseType.COMMON_GENERAL,
                                        "글쓰기는 필수",
                                        "{\"courseCodes\":[\"RGC1001\"],\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);
        var common = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);
        var secondary = service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR);

        assertThat(report.summary().graduated()).isEqualTo(writingPassed);
        assertThat(report.summary().achievementRate()).isEqualTo(writingPassed ? 100 : 0);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(common.unsatisfiedReasons())
                .containsExactlyElementsOf(writingPassed ? List.of() : List.of("글쓰기는 필수"));
        assertThat(secondary.unsatisfiedReasons()).isEmpty();
        assertMajorStatus(CourseType.COMMON_GENERAL, writingPassed, writingPassed ? 100 : 0);
        assertThat(report.areaOverviews()).extracting(area -> area.courseType()).containsExactly("COMMON_GENERAL");
        assertThat(common.creditStatus().earnedCredits()).isEqualTo(writingPassed ? 3 : 0);
        assertThat(secondary.creditStatus().earnedCredits()).isZero();
        assertThat(items(secondary)).isEmpty();
        if (writingPassed) {
            assertThat(items(common)).singleElement().satisfies(item -> {
                assertThat(item.title()).isEqualTo("글쓰기");
                assertThat(item.credit()).isEqualTo(3);
                assertThat(item.status()).isEqualTo("SATISFIED");
            });
        } else {
            assertThat(items(common)).isNotEmpty().allSatisfy(item -> {
                assertThat(item.title()).isEqualTo("글쓰기");
                assertThat(item.credit()).isZero();
                assertThat(item.status()).isEqualTo("UNSATISFIED");
            });
        }

        var transcript = transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow();
        var preview = service.preview(transcript);
        assertThat(preview.report()).isEqualTo(report);
        assertThat(preview.details().get(CourseType.COMMON_GENERAL)).isEqualTo(common);
        assertThat(preview.details()).doesNotContainKey(CourseType.SECOND_MAJOR);
    }

    @ParameterizedTest
    @CsvSource({"6,3", "3,6"})
    void 두_학과의_학문기초_사유는_같은_탭에_표시하고_동일_영역_목표는_더_큰_기준을_유지한다(int primaryMin, int secondaryMin) {
        givenTranscriptWithMajors(200L, null, null, null, passed("MAT1001", "미적분학", 3, "학기", null));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "MIN_CREDITS",
                        CourseType.ACADEMIC_FOUNDATION,
                        "수학 " + primaryMin + "학점",
                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"수학\"],\"minCredits\":" + primaryMin
                                + ",\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}")));
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(new GraduationRuleView(
                        2L,
                        "MIN_CREDITS",
                        CourseType.ACADEMIC_FOUNDATION,
                        "수학 " + secondaryMin + "학점",
                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"수학\"],\"minCredits\":" + secondaryMin
                                + ",\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        givenClassification("MAT1001", CourseType.ACADEMIC_FOUNDATION, "수학");

        var detail = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);

        assertThat(detail.creditStatus().targetCredits()).isEqualTo(6);
        assertThat(detail.creditStatus().remainingCredits()).isEqualTo(3);
        assertThat(detail.unsatisfiedReasons()).containsExactly("수학 6학점");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertMajorStatus(CourseType.ACADEMIC_FOUNDATION, false, 50);
        assertThat(service.getReport(MEMBER_ID).areaOverviews())
                .extracting(area -> area.courseType())
                .containsExactly("ACADEMIC_FOUNDATION");
        assertThat(detail.areaDetails()).singleElement().satisfies(area -> {
            assertThat(area.targetCredits()).isEqualTo(6);
            assertThat(area.satisfied()).isFalse();
        });
    }

    @ParameterizedTest
    @EnumSource(CourseType.class)
    void 복수전공_학과에만_있는_필수요건도_원래_이수구분에_표시한다(CourseType ruleCourseType) {
        givenTranscriptWithMajors(200L, null, null, null);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "REQUIRED_COURSE",
                        ruleCourseType,
                        "지정과목은 필수",
                        "{\"courseCodes\":[\"MISSING\"],\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        givenNoClassification();

        CourseType displayedType = ruleCourseType == CourseType.FIRST_MAJOR ? CourseType.SECOND_MAJOR : ruleCourseType;
        var report = service.getReport(MEMBER_ID);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(report.summary().graduated()).isFalse();
        assertThat(report.areaOverviews()).extracting(area -> area.courseType()).containsExactly(displayedType.name());
        assertMajorStatus(displayedType, false, 0);
        for (CourseType type : CourseType.values()) {
            assertThat(service.getAreaDetail(MEMBER_ID, type).unsatisfiedReasons())
                    .containsExactlyElementsOf(type == displayedType ? List.of("지정과목은 필수") : List.of());
        }
        var transcript = transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow();
        var preview = service.preview(transcript);
        assertThat(preview.report()).isEqualTo(report);
        assertThat(preview.details()).containsOnlyKeys(displayedType);
        assertThat(preview.details().get(displayedType)).isEqualTo(service.getAreaDetail(MEMBER_ID, displayedType));
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void 주전공_MS와_복수전공_MSC_목표를_합쳐도_각_규칙은_독립_판정한다(boolean primaryPassed) {
        // 전체 학문기초 학점은 같아도 전산학만 많이 이수하면 주전공의 수학·과학 요건은 미충족이다.
        givenTranscriptWithMajors(
                200L,
                null,
                null,
                null,
                passed("M", "수학과목", primaryPassed ? 12 : 3, "학기", "수학"),
                passed("S", "과학과목", primaryPassed ? 9 : 3, "학기", "과학"),
                passed("C", "전산학과목", primaryPassed ? 9 : 24, "학기", "전산학"),
                passed("B", "기본소양과목", 6, "학기", "기본소양"));
        given(curriculumLookupService.findCourseClassifications(anyList(), eq(ADMISSION_YEAR)))
                .willReturn(Map.of(
                        "M", new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "수학", null, null),
                        "S", new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "과학", null, null),
                        "C", new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "전산학", null, null),
                        "B", new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "기본소양", null, null)));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(
                        List.of(
                                new GraduationRuleView(
                                        1L,
                                        "MIN_CREDITS",
                                        CourseType.ACADEMIC_FOUNDATION,
                                        "수학과학 21학점",
                                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"수학\",\"과학\"],\"minCredits\":21,\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}"),
                                new GraduationRuleView(
                                        2L,
                                        "MIN_CREDITS",
                                        CourseType.ACADEMIC_FOUNDATION,
                                        "기본소양 6학점",
                                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"기본소양\"],\"minCredits\":6,\"applicableMajorRoles\":[\"DUAL_PRIMARY\"]}")));
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(
                        List.of(
                                new GraduationRuleView(
                                        1L,
                                        "MIN_CREDITS",
                                        CourseType.ACADEMIC_FOUNDATION,
                                        "수학과학전산학 30학점",
                                        "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"수학\",\"과학\",\"전산학\"],\"minCredits\":30,\"applicableMajorRoles\":[\"SECONDARY\"]}")));

        var report = service.getReport(MEMBER_ID);
        var detail = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);
        assertThat(detail.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(36, 36, 0));
        assertThat(detail.unsatisfiedReasons())
                .containsExactlyElementsOf(primaryPassed ? List.of() : List.of("수학과학 21학점"));
        assertThat(report.summary().graduated()).isEqualTo(primaryPassed);
        assertThat(report.summary().achievementRate()).isEqualTo(primaryPassed ? 100 : 66);
        assertThat(report.areaOverviews()).singleElement().satisfies(area -> {
            assertThat(area.courseType()).isEqualTo("ACADEMIC_FOUNDATION");
            assertThat(area.remainingCredits()).isZero();
            assertThat(area.satisfied()).isEqualTo(primaryPassed);
        });
        assertThat(detail.areaDetails())
                .allSatisfy(area -> assertThat(area.targetCredits()).isEqualTo("기본소양".equals(area.areaName()) ? 6 : 0));
        var preview = service.preview(
                transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow());
        assertThat(preview.report()).isEqualTo(report);
        assertThat(preview.details())
                .containsOnlyKeys(CourseType.ACADEMIC_FOUNDATION)
                .containsEntry(CourseType.ACADEMIC_FOUNDATION, detail);
    }

    @Test
    void 복수전공_필수과목을_주전공에서_이수해도_주전공_화면의_필수_충족으로_표시하지_않는다() {
        givenTranscriptWithMajors(200L, null, null, null, passed("DAI1001", "인공지능", 3, "전공", "전문"));
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, false);
        GraduationRuleView secondaryRequired = new GraduationRuleView(
                3L,
                "REQUIRED_COURSE",
                CourseType.FIRST_MAJOR,
                "인공지능은 필수",
                "{\"courseCodes\":[\"DAI1001\"],\"applicableMajorRoles\":[\"SECONDARY\"]}");
        given(curriculumLookupService.findGraduationRules(20L)).willReturn(List.of(secondaryRequired));
        givenNoClassification();

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

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

    @Test
    void 주전공_학과_미식별은_요약과_상세_모두_미지원으로_반환한다() {
        // 복수전공 ID가 있어도 주전공을 대신하여 그 세트를 사용하지 않는다.
        TranscriptView transcript = new TranscriptView(
                1L,
                MEMBER_ID,
                null,
                200L,
                null,
                null,
                null,
                ADMISSION_YEAR,
                "학사과정",
                false,
                3,
                new BigDecimal("4.50"),
                null,
                false,
                null,
                null,
                false,
                false,
                false,
                true,
                false,
                List.of(passed("CSE1", "과목", 3, "전공", "기초")));
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));

        assertThatThrownBy(() -> service.getReport(MEMBER_ID))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
        assertThatThrownBy(() -> service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
        verifyNoInteractions(curriculumLookupService);
    }
}
