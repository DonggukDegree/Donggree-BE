package com.donggree.curriculum.internal.domain.graduationrule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GraduationRuleConfigValidatorTest {

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
