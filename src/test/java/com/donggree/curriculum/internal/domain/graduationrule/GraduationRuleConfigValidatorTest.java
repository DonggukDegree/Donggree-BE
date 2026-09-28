package com.donggree.curriculum.internal.domain.graduationrule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GraduationRuleConfigValidatorTest {

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
