package com.donggree.graduation.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MajorRoleRuleMatcherTest {

    @Test
    void 역할_옵션이_없으면_단일전공_규칙으로_해석한다() {
        GraduationRuleView rule = rule("MIN_CREDITS", "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":72}");

        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SINGLE_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.DUAL_PRIMARY)).isFalse();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SECONDARY)).isFalse();
    }

    @Test
    void 설정한_역할에만_규칙이_적용된다() {
        GraduationRuleView rule = rule(
                "REQUIRED_COURSE",
                "{\"courseCodes\":[\"CSE1001\"],\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"SECONDARY\"]}");

        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SINGLE_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.DUAL_PRIMARY)).isFalse();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SECONDARY)).isTrue();
    }

    @Test
    void 지원하지_않는_규칙_유형은_복수전공_학과에_재적용되지_않는다() {
        GraduationRuleView rule = rule("TOTAL_CREDITS", "{\"minCredits\":130}");

        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SINGLE_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.DUAL_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SECONDARY)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"applicableMajorRoles\":[]}", "{\"applicableMajorRoles\":null}"})
    void 기존_THESIS는_두_주전공_역할을_유지하고_복수전공에는_적용하지_않는다(String config) {
        GraduationRuleView rule = rule("THESIS", config);

        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SINGLE_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.DUAL_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SECONDARY)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(MajorRole.class)
    void THESIS도_명시적으로_선택한_역할에만_적용된다(MajorRole selected) {
        GraduationRuleView rule = rule("THESIS", "{\"applicableMajorRoles\":[\"" + selected.name() + "\"]}");

        for (MajorRole role : MajorRole.values()) {
            assertThat(MajorRoleRuleMatcher.applies(rule, role)).isEqualTo(role == selected);
        }
    }

    @Test
    void THESIS의_여러_역할_선택을_지원한다() {
        GraduationRuleView rule = rule("THESIS", "{\"applicableMajorRoles\":[\"DUAL_PRIMARY\",\"SECONDARY\"]}");

        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SINGLE_PRIMARY)).isFalse();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.DUAL_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SECONDARY)).isTrue();
    }

    private GraduationRuleView rule(String typeName, String config) {
        return new GraduationRuleView(1L, typeName, CourseType.FIRST_MAJOR, "테스트 규칙", config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"minCount\":4}", "{\"applicableMajorRoles\":null}", "{\"applicableMajorRoles\":[]}"})
    void 기존_영어강의는_두_주전공_역할에_유지하고_복수전공에는_추가하지_않는다(String config) {
        var rule = rule("ENGLISH_COURSE", config);

        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SINGLE_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.DUAL_PRIMARY)).isTrue();
        assertThat(MajorRoleRuleMatcher.applies(rule, MajorRole.SECONDARY)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(MajorRole.class)
    void 영어강의도_선택한_역할에만_적용한다(MajorRole selected) {
        var rule = rule("ENGLISH_COURSE", "{\"minCount\":4,\"applicableMajorRoles\":[\"" + selected.name() + "\"]}");

        for (MajorRole role : MajorRole.values()) {
            assertThat(MajorRoleRuleMatcher.applies(rule, role)).isEqualTo(role == selected);
        }
    }

    @Test
    void 영어강의_규칙에서_세_역할을_함께_선택할_수_있다() {
        var rule = rule(
                "ENGLISH_COURSE",
                "{\"minCount\":4,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]}");

        for (MajorRole role : MajorRole.values()) {
            assertThat(MajorRoleRuleMatcher.applies(rule, role)).isTrue();
        }
    }
}
