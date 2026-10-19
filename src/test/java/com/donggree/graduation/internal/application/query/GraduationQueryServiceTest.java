package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.graduation.internal.application.exception.GraduationErrorCode;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel.CourseItem;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GraduationQueryServiceTest extends GraduationQueryTestSupport {

    @Test
    void 심화과정_학생은_공학인증심화대상_여부를_넘겨_세트를_조회한다() {
        givenTranscriptOf(true, null, null, null, null);
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, true);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of());

        assertThat(service.getReport(MEMBER_ID)).isNotNull();
    }

    @Test
    void 심화과정_학생에게_맞는_세트가_없으면_미지원_학과와_동일하게_리포트_생성이_실패한다() {
        // 일반과정 세트만 등록된 학과. 그 요건으로 대신 판정하지 않고 실패시킨다.
        givenTranscriptOf(true, null, null, null, null);
        willReturn(Optional.empty())
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, true);

        assertThatThrownBy(() -> service.getReport(MEMBER_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
    }

    @ParameterizedTest
    @CsvSource({
        "true,true,false", "true,false,false", "false,true,false", "false,false,false",
        "true,true,true", "true,false,true", "false,true,true", "false,false,true"
    })
    void 두_학과의_영어강의는_독립_판정하고_규칙의_이수구분에_따라_표시한다(boolean primaryPassed, boolean secondaryPassed, boolean majorRule) {
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
                false,
                false,
                records);
        given(transcriptLookupService.findByMemberId(MEMBER_ID)).willReturn(Optional.of(transcript));
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
        // 옵션 없는 기존 영어강의는 복수전공자의 주전공에서도 유지한다.
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L,
                                "ENGLISH_COURSE",
                                majorRule ? CourseType.FIRST_MAJOR : null,
                                "전공 영어강의 2과목",
                                "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2}"),
                        new GraduationRuleView(
                                2L,
                                "ENGLISH_COURSE",
                                null,
                                "단일전공 전용 요건",
                                "{\"minCount\":99,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}")));
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, true);
        given(curriculumLookupService.findGraduationRules(20L))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L,
                                "ENGLISH_COURSE",
                                majorRule ? CourseType.FIRST_MAJOR : null,
                                "전공 영어강의 2과목",
                                "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":2,\"applicableMajorRoles\":[\"SECONDARY\"]}"),
                        new GraduationRuleView(3L, "ENGLISH_COURSE", null, "B학과 옵션 없는 영어강의", "{\"minCount\":99}"),
                        new GraduationRuleView(4L, "TOTAL_CREDITS", null, "B학과 총학점 140", "{\"minCredits\":140}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);

        assertThat(report.summary().graduated()).isEqualTo(primaryPassed && secondaryPassed);
        assertThat(report.summary().achievementRate())
                .isEqualTo(((primaryPassed ? 1 : 0) + (secondaryPassed ? 1 : 0)) * 50);
        assertThat(report.summary().unsatisfiedReasons())
                .containsExactlyElementsOf(
                        !majorRule && !(primaryPassed && secondaryPassed) ? List.of("전공 영어강의 2과목") : List.of());
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .containsExactlyElementsOf(majorRule && !primaryPassed ? List.of("전공 영어강의 2과목") : List.of());
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .containsExactlyElementsOf(majorRule && !secondaryPassed ? List.of("전공 영어강의 2과목") : List.of());
        if (majorRule) {
            assertMajorStatus(CourseType.FIRST_MAJOR, primaryPassed, primaryPassed ? 100 : 0);
            assertMajorStatus(CourseType.SECOND_MAJOR, secondaryPassed, secondaryPassed ? 100 : 0);
        } else {
            if (primaryPassed) assertMajorStatus(CourseType.FIRST_MAJOR, true, 100);
            else
                assertThat(report.areaOverviews())
                        .extracting(area -> area.courseType())
                        .doesNotContain("FIRST_MAJOR");
            if (secondaryPassed) assertMajorStatus(CourseType.SECOND_MAJOR, true, 100);
            else
                assertThat(report.areaOverviews())
                        .extracting(area -> area.courseType())
                        .doesNotContain("SECOND_MAJOR");
        }
        assertThat(report.hasUnsupportedMajor()).isFalse();
        var preview = service.preview(transcript);
        assertThat(preview.report()).isEqualTo(report);
        preview.details()
                .forEach((type, detail) -> assertThat(detail).isEqualTo(service.getAreaDetail(MEMBER_ID, type)));
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
        assertThat(report.summary().unsatisfiedReasons()).containsExactly("총학점 130 이상", "평점 4.5 이상", "졸업시험 합격");
        assertThat(report.summary().achievementRate()).isZero();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL).unsatisfiedReasons())
                .containsExactly("EAS1 선이수");
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
    }

    @Test
    void 두_전공의_사유를_이수구분별로_표시해도_추가_요건과_전체_졸업률을_유지한다() {
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
        willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(200L, ADMISSION_YEAR, true);
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
        assertThat(report.summary().unsatisfiedReasons()).containsExactly("졸업시험 합격");
        assertMajorStatus(CourseType.FIRST_MAJOR, true, 100);
        assertMajorStatus(CourseType.SECOND_MAJOR, false, 0);
        assertThat(primary.unsatisfiedReasons()).isEmpty();
        assertThat(secondary.unsatisfiedReasons()).containsExactly("전공 6학점");
        assertThat(academic.unsatisfiedReasons()).isEmpty();
        assertThat(primary.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(3, 3, 0));
        assertThat(secondary.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(3, 6, 3));
        assertThat(academic.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(3, 3, 0));
        assertThat(items(primary)).extracting(CourseItem::title).containsExactly("주전공과목");
        assertThat(items(secondary)).extracting(CourseItem::title).containsExactly("복수전공과목");
        assertThat(items(academic)).extracting(CourseItem::title).containsExactly("미적분학");
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "true,true,false", "true,false,false", "false,false,true"})
    void 미리보기와_사용자_요약_전체_영역_상세의_동일한_판정(boolean dual, boolean secondarySet, boolean transfer) {
        givenTranscriptOf(
                false,
                dual ? 200L : null,
                null,
                null,
                null,
                transfer,
                passed("CSE3001", "주전공과목", 3, "전공", "전문"),
                passed("ENE2001", "복수전공과목", 3, "복수1", "기초"));
        willReturn(Optional.of(new RequirementSetView(REQUIREMENT_SET_ID, DEPARTMENT_ID, 2020, 2025)))
                .given(curriculumLookupService)
                .findActiveRequirementSet(DEPARTMENT_ID, ADMISSION_YEAR, false);
        givenNoClassification();
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(new GraduationRuleView(
                        1L,
                        "MIN_CREDITS",
                        CourseType.FIRST_MAJOR,
                        "주전공 6학점",
                        "{\"minCredits\":6,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\"]}")));
        if (dual) {
            willReturn(secondarySet ? Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)) : Optional.empty())
                    .given(curriculumLookupService)
                    .findActiveRequirementSet(200L, ADMISSION_YEAR, true);
        }
        if (secondarySet) {
            given(curriculumLookupService.findGraduationRules(20L))
                    .willReturn(List.of(new GraduationRuleView(
                            2L,
                            "THESIS",
                            CourseType.FIRST_MAJOR,
                            "복수전공 시험",
                            "{\"applicableMajorRoles\":[\"SECONDARY\"]}")));
        }
        var transcript = transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow();
        clearInvocations(transcriptLookupService, curriculumLookupService);
        var preview = service.preview(transcript);
        verifyNoInteractions(transcriptLookupService);
        verify(curriculumLookupService).findGraduationRules(REQUIREMENT_SET_ID);
        assertThat(preview.report()).isEqualTo(service.getReport(MEMBER_ID));
        assertThat(preview.report().hasUnsupportedMajor()).isEqualTo(transfer || (dual && !secondarySet));
        assertThat(preview.details().keySet())
                .extracting(Enum::name)
                .containsExactlyElementsOf(preview.report().areaOverviews().stream()
                        .map(area -> area.courseType())
                        .toList());
        preview.details()
                .forEach((type, detail) -> assertThat(detail).isEqualTo(service.getAreaDetail(MEMBER_ID, type)));
    }

    @Test
    void 미리보기에도_주전공_활성_세트_필수() {
        givenTranscriptOf(false, null, null, null, null);
        givenNoClassification();
        var transcript = transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow();
        clearInvocations(transcriptLookupService);
        assertThatThrownBy(() -> service.preview(transcript))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(GraduationErrorCode.REQUIREMENT_SET_NOT_FOUND));
        verifyNoInteractions(transcriptLookupService);
    }

    @org.junit.jupiter.api.Test
    void 적용_역할에서_제외된_규칙의_판정_설정을_해석하지_않는다() {
        givenTranscript();
        givenNoClassification();
        givenMinCreditsRule(
                com.donggree.curriculum.CourseType.FIRST_MAJOR,
                "복수전공 전용",
                "{\"applicableMajorRoles\":[\"SECONDARY\"],\"minCredits\":{\"invalid\":true}}");
        org.assertj.core.api.Assertions.assertThat(
                        service.getReport(MEMBER_ID).summary().achievementRate())
                .isZero();
    }
}
