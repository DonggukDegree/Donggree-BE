package com.donggree.graduation.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.GraduationRuleView;
import org.junit.jupiter.api.Test;

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

    private GraduationRuleView rule(String typeName, String config) {
        return new GraduationRuleView(1L, typeName, CourseType.FIRST_MAJOR, "테스트 규칙", config);
    }
}
