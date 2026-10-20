package com.donggree.curriculum.internal.domain.graduationrule;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GraduationRuleConfigValidatorTest {

    @Test
    void 그룹별_학점과_충족_그룹_수를_허용하고_기존_합산_설정도_유지한다() {
        assertThat(
                        GraduationRuleConfigValidator.hasValidCreditGroups(
                                "MIN_CREDITS",
                                """
                {"creditGroups":[{"courseCodes":["ABC*","7777700"],"minCredits":27},
                                 {"courseCodes":["DEF*"],"minCredits":24}],"minSatisfiedGroups":2}
                """))
                .isTrue();
        assertThat(GraduationRuleConfigValidator.hasValidCreditGroups("MIN_CREDITS", "{\"minCredits\":27}"))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"creditGroups\":[]}",
                "{\"creditGroups\":[{\"courseCodes\":[],\"minCredits\":27}],\"minSatisfiedGroups\":1}",
                "{\"creditGroups\":[{\"courseCodes\":[\"ABC*\"],\"minCredits\":0}],\"minSatisfiedGroups\":1}",
                "{\"creditGroups\":[{\"courseCodes\":[\"ABC*\"],\"minCredits\":1.5}],\"minSatisfiedGroups\":1}",
                "{\"creditGroups\":[{\"courseCodes\":[\"ABC*\"],\"minCredits\":27}],\"minSatisfiedGroups\":0}",
                "{\"creditGroups\":[{\"courseCodes\":[\"ABC*\"],\"minCredits\":27}],\"minSatisfiedGroups\":2}",
                "{\"creditGroups\":[{\"courseCodes\":[\"ABC*\"],\"minCredits\":27}],\"minSatisfiedGroups\":1.5}",
                "{\"minSatisfiedGroups\":1}",
                "{\"creditGroups\":null,\"minSatisfiedGroups\":1}"
            })
    void 잘못된_그룹_구조와_하한과_충족_수를_거절한다(String config) {
        assertThat(GraduationRuleConfigValidator.hasValidCreditGroups("MIN_CREDITS", config))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC*", "ABC1*", "ABC1001", "AB*"})
    void 다른_그룹의_동일_접두어와_포함되는_접두어와_개별_코드를_거절한다(String second) {
        String config =
                """
                {"creditGroups":[{"courseCodes":["ABC*"],"minCredits":27},
                                 {"courseCodes":["%s"],"minCredits":27}],"minSatisfiedGroups":2}
                """
                        .formatted(second);
        assertThat(GraduationRuleConfigValidator.hasValidCreditGroups("MIN_CREDITS", config))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc*", "*", "AB*C", "ABC-01", "ABCDEFGHI", "ABC**", " ABC*"})
    void 그룹_학수번호의_잘못된_형식을_거절한다(String pattern) {
        String config =
                """
                {"creditGroups":[{"courseCodes":["%s"],"minCredits":27}],"minSatisfiedGroups":1}
                """
                        .formatted(pattern);
        assertThat(GraduationRuleConfigValidator.hasValidCreditGroups("MIN_CREDITS", config))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "\"minCredits\":54",
                "\"minCount\":2",
                "\"courseCodes\":[\"ABC*\"]",
                "\"creditSource\":\"TEACHING_SUMMARY\"",
                "\"creditAdjustments\":[{\"requiredCourseCodes\":[\"A\"],\"replacementCourseCodes\":[\"B\"],\"credits\":3}]"
            })
    void 그룹_설정과_합산_임계값_및_감면의_혼용을_거절한다(String other) {
        String config =
                """
                {"creditGroups":[{"courseCodes":["ABC*"],"minCredits":27}],"minSatisfiedGroups":1,%s}
                """
                        .formatted(other);
        assertThat(GraduationRuleConfigValidator.hasValidCreditGroups("MIN_CREDITS", config))
                .isFalse();
    }

    @Test
    void 교직_요약학점_기준은_제1전공_범위에서_양의_최소학점으로_설정한다() {
        assertThat(GraduationRuleConfigValidator.hasValidCreditSource(
                        "MIN_CREDITS",
                        CourseType.FIRST_MAJOR,
                        "{\"creditSource\":\"TEACHING_SUMMARY\",\"minCredits\":22}"))
                .isTrue();
        assertThat(GraduationRuleConfigValidator.hasValidCreditSource(
                        "MIN_CREDITS",
                        CourseType.SECOND_MAJOR,
                        "{\"creditSource\":\"TEACHING_SUMMARY\",\"minCredits\":22}"))
                .isFalse();
        assertThat(GraduationRuleConfigValidator.hasValidCreditSource(
                        "MIN_CREDITS",
                        CourseType.FIRST_MAJOR,
                        "{\"creditSource\":\"TEACHING_SUMMARY\",\"minCredits\":0}"))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"courseCodes\":[\"A\",\"A_OLD\"]}",
                "{\"requiredCourseSets\":[[[\"PHY1\",\"OLD_PHY1\"],[\"PHY2\"]],[[\"BIO1\"],[\"BIO2\"]]]}",
                "{\"requiredCourseSets\":[[[\"DAI*\"]]]}"
            })
    void 필수과목은_기존_OR_목록이나_과목_세트를_저장할_수_있다(String config) {
        assertThat(GraduationRuleConfigValidator.hasValidRequiredCourses("REQUIRED_COURSE", config))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "null",
                "[]",
                "{\"courseCodes\":[]}",
                "{\"courseCodes\":[null]}",
                "{\"requiredCourseSets\":[]}",
                "{\"requiredCourseSets\":[[]]}",
                "{\"requiredCourseSets\":[[[]]]}",
                "{\"requiredCourseSets\":[[[\" \"]]]}",
                "{\"requiredCourseSets\":[[[null]]]}",
                "{\"requiredCourseSets\":[[[1]]]}",
                "{\"requiredCourseSets\":[[\"A\",\"B\"]]}",
                "{\"requiredCourseSets\":[[[\"A\"]],[]]}",
                "{\"requiredCourseSets\":[[[\"A\"]]],\"courseCodes\":[\"B\"]}"
            })
    void 필수과목의_빈_세트와_잘못된_구조와_두_방식의_혼용은_거절한다(String config) {
        assertThat(GraduationRuleConfigValidator.hasValidRequiredCourses("REQUIRED_COURSE", config))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"applicableMajorRoles\":null}",
                "{\"applicableMajorRoles\":[]}",
                "{\"applicableMajorRoles\":\"SECONDARY\"}",
                "{\"applicableMajorRoles\":[\"MINOR\"]}",
                "{\"applicableMajorRoles\":[null]}",
                "{\"applicableMajorRoles\":[1]}"
            })
    void THESIS_저장에는_허용된_적용_대상이_하나_이상_필요하다(String config) {
        assertThat(GraduationRuleConfigValidator.hasValidMajorRoles("THESIS", config))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SINGLE_PRIMARY", "DUAL_PRIMARY", "SECONDARY"})
    void THESIS의_세_적용_대상을_허용한다(String role) {
        assertThat(GraduationRuleConfigValidator.hasValidMajorRoles(
                        "THESIS", "{\"applicableMajorRoles\":[\"" + role + "\"]}"))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"applicableMajorRoles\":null}",
                "{\"applicableMajorRoles\":[]}",
                "{\"applicableMajorRoles\":\"SECONDARY\"}",
                "{\"applicableMajorRoles\":[\"MINOR\"]}",
                "{\"applicableMajorRoles\":[null]}",
                "{\"applicableMajorRoles\":[1]}"
            })
    void 영어강의_저장에도_유효한_적용_대상이_필요하다(String config) {
        assertThat(GraduationRuleConfigValidator.hasValidMajorRoles("ENGLISH_COURSE", config))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SINGLE_PRIMARY", "DUAL_PRIMARY", "SECONDARY"})
    void 영어강의의_세_적용_대상을_허용한다(String role) {
        assertThat(GraduationRuleConfigValidator.hasValidMajorRoles(
                        "ENGLISH_COURSE", "{\"applicableMajorRoles\":[\"" + role + "\"]}"))
                .isTrue();
    }
}
