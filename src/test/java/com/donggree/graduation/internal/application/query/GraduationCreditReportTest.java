package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import org.junit.jupiter.api.Test;

class GraduationCreditReportTest extends GraduationQueryTestSupport {

    @Test
    void 다중_영역_규칙일_때_개별_영역_섹션의_목표학점은_0이다() {
        givenTranscript(passed("RGC1075", "동남아지역연구", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "21세기시민·지역연구·미래위험사회와안전 중 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"21세기시민\",\"지역연구\",\"미래위험사회와안전\"],"
                        + "\"minCredits\":2}");
        givenClassification("RGC1075", CourseType.COMMON_GENERAL, "지역연구");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.areaDetails())
                .filteredOn(section -> section.areaName().equals("지역연구"))
                .singleElement()
                .satisfies(section -> assertThat(section.targetCredits()).isZero());
    }

    @Test
    void 단일_영역_규칙의_목표학점은_섹션에_그대로_표시된다() {
        givenTranscript(passed("RGC1090", "리더십과봉사", 1, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "리더십을 2학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"areaNames\":[\"리더십\"],\"minCredits\":2}");
        givenClassification("RGC1090", CourseType.COMMON_GENERAL, "리더십");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.areaDetails())
                .filteredOn(section -> section.areaName().equals("리더십"))
                .singleElement()
                .satisfies(section -> {
                    assertThat(section.targetCredits()).isEqualTo(2);
                    assertThat(section.satisfied()).isFalse();
                });
    }

    @Test
    void 이수구분_전체_규칙은_creditStatus의_목표학점이_된다() {
        givenTranscript(passed("RGC0003", "불교와인간", 2, "공교", null));
        givenMinCreditsRule(
                CourseType.COMMON_GENERAL,
                "공통교양을 17학점 이상 이수해야 합니다.",
                "{\"courseType\":\"COMMON_GENERAL\",\"minCredits\":17}");
        givenClassification("RGC0003", CourseType.COMMON_GENERAL, "자아성찰");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.COMMON_GENERAL);

        assertThat(response.creditStatus().targetCredits()).isEqualTo(17);
        assertThat(response.creditStatus().earnedCredits()).isEqualTo(2);
        assertThat(response.creditStatus().remainingCredits()).isEqualTo(15);
    }

    @Test
    void minCredits가_없는_규칙은_목표학점에_반영되지_않는다() {
        givenTranscript(passed("PRI4002", "일반물리학및실험1", 3, "학기", null));
        givenMinCreditsRule(
                CourseType.ACADEMIC_FOUNDATION,
                "실험 교과목 중 1과목을 필수 선택해야 합니다.",
                "{\"courseType\":\"ACADEMIC_FOUNDATION\",\"subCategories\":[\"실험\"],\"minCount\":1}");
        givenClassification("PRI4002", CourseType.ACADEMIC_FOUNDATION, "과학");

        AreaDetailReadModel response = service.getAreaDetail(MEMBER_ID, CourseType.ACADEMIC_FOUNDATION);

        assertThat(response.creditStatus().targetCredits()).isZero();
    }
}
