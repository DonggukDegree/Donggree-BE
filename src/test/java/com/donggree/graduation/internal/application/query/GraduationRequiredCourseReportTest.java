package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GraduationRequiredCourseReportTest extends GraduationQueryTestSupport {

    @ParameterizedTest
    @CsvSource({"false,PHY2,true", "false,BIO2,false", "true,PHY2,true", "true,BIO2,false"})
    void 필수과목_세트는_완성_여부를_학문기초에_표시하고_미리보기에도_동일하게_반영한다(boolean dualMajor, String secondCode, boolean expected) {
        givenTranscriptWithMajors(
                dualMajor ? 200L : null,
                null,
                null,
                null,
                passed("PHY1", "물리1", 3, "학기", "과학"),
                passed(secondCode, "두번째실험", 3, "학기", "과학"));
        var rule = new GraduationRuleView(
                1L,
                "REQUIRED_COURSE",
                CourseType.ACADEMIC_FOUNDATION,
                "같은 분야 실험 1·2를 모두 이수해야 합니다.",
                """
                {"requiredCourseSets":[[["PHY1","OLD_PHY1"],["PHY2"]],[["BIO1"],["BIO2"]]],
                 "applicableMajorRoles":["SINGLE_PRIMARY","DUAL_PRIMARY","SECONDARY"]}
                """);
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
        if (dualMajor) {
            willReturn(Optional.of(new RequirementSetView(20L, 200L, 2020, 2025)))
                    .given(curriculumLookupService)
                    .findActiveRequirementSet(200L, ADMISSION_YEAR, true);
            given(curriculumLookupService.findGraduationRules(20L)).willReturn(List.of(rule));
        }
        var classification = new CourseClassificationView(CourseType.ACADEMIC_FOUNDATION, "과학", "실험", null);
        given(curriculumLookupService.findCourseClassifications(anyList(), eq(ADMISSION_YEAR)))
                .willReturn(Map.of(
                        "PHY1",
                        classification,
                        "OLD_PHY1",
                        classification,
                        "PHY2",
                        classification,
                        "BIO1",
                        classification,
                        "BIO2",
                        classification));

        var report = service.getReport(MEMBER_ID);
        var detail = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);
        assertThat(report.summary().graduated()).isEqualTo(expected);
        assertThat(report.summary().achievementRate()).isEqualTo(expected ? 100 : 0);
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(detail.unsatisfiedReasons())
                .containsExactlyElementsOf(expected ? List.of() : List.of(rule.ruleName()));
        assertThat(detail.areaDetails()).singleElement().satisfies(area -> {
            assertThat(area.areaName()).isEqualTo("과학");
            assertThat(area.satisfied()).isEqualTo(expected);
            assertThat(area.earnedCredits()).isEqualTo(6);
            assertThat(area.targetCredits()).isZero();
        });
        assertThat(items(detail))
                .filteredOn(item -> item.title().equals("물리1"))
                .singleElement()
                .satisfies(item -> assertThat(item.status()).isEqualTo("SATISFIED"));
        if (expected) {
            assertThat(items(detail)).noneMatch(item -> "UNSATISFIED".equals(item.status()));
        } else {
            assertThat(items(detail)).anySatisfy(item -> {
                assertThat(item.title()).isEqualTo(rule.ruleName());
                assertThat(item.status()).isEqualTo("UNSATISFIED");
            });
        }
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();
        var preview = service.preview(
                transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow());
        assertThat(preview.report()).isEqualTo(report);
        assertThat(preview.details().get(CourseType.ACADEMIC_FOUNDATION)).isEqualTo(detail);
    }

    @Test
    void 미수강_과목_세트도_등록된_과목_영역에서_미충족으로_표시한다() {
        givenTranscript();
        var rule = new GraduationRuleView(
                1L,
                "REQUIRED_COURSE",
                CourseType.ACADEMIC_FOUNDATION,
                "실험 세트 이수",
                "{\"requiredCourseSets\":[[[\"PHY1\"],[\"PHY2\"]]]}");
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID)).willReturn(List.of(rule));
        givenClassification("PHY1", CourseType.ACADEMIC_FOUNDATION, "과학");

        var detail = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);
        verify(curriculumLookupService).findCourseClassifications(List.of("PHY1", "PHY2"), ADMISSION_YEAR);
        assertThat(detail.areaDetails()).singleElement().satisfies(area -> {
            assertThat(area.areaName()).isEqualTo("과학");
            assertThat(area.satisfied()).isFalse();
            assertThat(area.earnedCredits()).isZero();
        });
    }

    @Test
    void 다른_이수구분으로_충족한_필수과목은_실제_이수_탭에_충족으로_표시된다() {
        givenTranscript(passed("CSE3001", "운영체제특론", 3, "공교", null));
        givenRequiredRule("CSE3001", "운영체제는 필수");
        givenClassification("CSE3001", CourseType.COMMON_GENERAL, "기초세계");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(items(response)).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("운영체제특론");
            assertThat(item.status()).isEqualTo("SATISFIED");
            assertThat(item.credit()).isEqualTo(3);
        });
    }

    @Test
    void 다른_이수구분으로_충족한_필수과목은_규칙_탭에_중복_표시되지_않는다() {
        givenTranscript(passed("CSE3001", "운영체제특론", 3, "공교", null));
        givenRequiredRule("CSE3001", "운영체제는 필수");
        givenClassification("CSE3001", CourseType.COMMON_GENERAL, "기초세계");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response)).noneMatch(item -> item.title().contains("운영체제"));
    }

    @Test
    void 미이수_필수과목은_규칙_탭에_불충족_placeholder로_표시된다() {
        givenTranscript(passed("CSE9999", "자료구조", 3, "전공", "전문"));
        givenRequiredRule("CSE3001", "운영체제는 필수");
        givenNoClassification();

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response)).anySatisfy(item -> {
            assertThat(item.title()).isEqualTo("운영체제");
            assertThat(item.status()).isEqualTo("UNSATISFIED");
            assertThat(item.credit()).isEqualTo(0);
        });
    }

    @Test
    void 필수과목_표시명은_을_이수_표현을_제거한다() {
        givenTranscript(passed("CSE9999", "자료구조", 3, "전공", "전문"));
        givenRequiredRule("CSE3001", "통치구조론을 이수하거나 헌법 졸업시험을 응시해야 합니다.");
        givenNoClassification();

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);

        assertThat(items(response)).anySatisfy(item -> assertThat(item.title()).isEqualTo("통치구조론"));
    }

    @Test
    void 단일전공의_교양_규칙_사유와_학점은_교양에_유지한다() {
        givenTranscript(passed("RGC1075", "동남아지역연구", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"21세기시민\",\"지역연구\",\"미래위험사회와안전\"],"
                        + "\"minCredits\":2}");
        givenClassification("RGC1075", CourseType.COMMON_GENERAL, "지역연구");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        var primary = service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR);
        assertThat(response.unsatisfiedReasons()).containsExactly("21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.");
        assertThat(primary.unsatisfiedReasons()).isEmpty();
        assertThat(response.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(1, 2, 1));
        assertThat(primary.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(0, 0, 0));
        assertThat(items(primary)).isEmpty();
        assertMajorStatus(CourseType.COMMON_GENERAL, false, 0);
        assertThat(service.getReport(MEMBER_ID).areaOverviews())
                .extracting(area -> area.courseType())
                .containsExactly("COMMON_GENERAL");
    }

    @ParameterizedTest
    @CsvSource({
        "COMMON_GENERAL,공교,false", "COMMON_GENERAL,공교,true",
        "ACADEMIC_FOUNDATION,학기,false", "ACADEMIC_FOUNDATION,학기,true"
    })
    void 교양과_학문기초의_최소학점과_필수과목은_역할과_무관하게_원래_탭에_표시한다(CourseType type, String pdfType, boolean dualPrimary) {
        givenTranscriptWithMajors(
                dualPrimary ? 200L : null, null, null, null, passed("BASIC1", "기본과목", 3, pdfType, null));
        if (dualPrimary) {
            willReturn(Optional.empty())
                    .given(curriculumLookupService)
                    .findActiveRequirementSet(200L, ADMISSION_YEAR, true);
        }
        String roles = "\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]";
        String creditReason = type == CourseType.ACADEMIC_FOUNDATION ? "기본소양 6학점 이상 이수" : "공통교양 6학점 이상 이수";
        given(curriculumLookupService.findGraduationRules(REQUIREMENT_SET_ID))
                .willReturn(List.of(
                        new GraduationRuleView(
                                1L,
                                "MIN_CREDITS",
                                type,
                                creditReason,
                                "{\"courseType\":\"" + type.name() + "\",\"minCredits\":6," + roles + "}"),
                        new GraduationRuleView(
                                2L,
                                "REQUIRED_COURSE",
                                type,
                                "추가 기본과목은 필수",
                                "{\"courseCodes\":[\"BASIC2\"]," + roles + "}")));
        givenNoClassification();

        var report = service.getReport(MEMBER_ID);
        var detail = service.getAreaDetail(MEMBER_ID, type);
        assertThat(report.areaOverviews()).extracting(area -> area.courseType()).containsExactly(type.name());
        assertThat(report.summary().unsatisfiedReasons()).isEmpty();
        assertThat(report.summary().achievementRate()).isZero();
        assertThat(report.summary().graduated()).isFalse();
        assertThat(detail.unsatisfiedReasons()).containsExactly(creditReason, "추가 기본과목은 필수");
        assertThat(detail.creditStatus()).isEqualTo(new AreaDetailReadModel.CreditStatus(3, 6, 3));
        assertMajorStatus(type, false, 0);
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.FIRST_MAJOR).unsatisfiedReasons())
                .isEmpty();
        assertThat(service.getAreaDetail(MEMBER_ID, CourseType.SECOND_MAJOR).unsatisfiedReasons())
                .isEmpty();

        var preview = service.preview(
                transcriptLookupService.findByMemberId(MEMBER_ID).orElseThrow());
        assertThat(preview.report()).isEqualTo(report);
        assertThat(preview.details()).containsOnlyKeys(type).containsEntry(type, detail);
    }
}
