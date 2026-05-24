package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequirementSetTest {

    @Test
    void 졸업요건세트_생성_시_필드가_올바르게_저장된다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.getDepartmentId()).isEqualTo(1L);
        assertThat(set.getYearStart()).isEqualTo(2023);
        assertThat(set.getYearEnd()).isEqualTo(2025);
        assertThat(set.getVersion()).isEqualTo(1);
        assertThat(set.getDescription()).isEqualTo("컴퓨터·AI학부 23~25학번 졸업 요건");
        assertThat(set.isActive()).isTrue();
        assertThat(set.getRules()).isEmpty();
    }

    @Test
    void departmentId가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(null, 2023, 2025, 1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void yearStart가_yearEnd보다_크면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(1L, 2025, 2023, 1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void version이_0_이하이면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(1L, 2023, 2025, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> RequirementSet.create(1L, 2023, 2025, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- appliesTo 테스트 ---

    @Test
    void 입학년도가_범위_내이면_appliesTo는_true를_반환한다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.appliesTo(2023)).isTrue();
        assertThat(set.appliesTo(2024)).isTrue();
        assertThat(set.appliesTo(2025)).isTrue();
    }

    @Test
    void 입학년도가_범위_밖이면_appliesTo는_false를_반환한다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.appliesTo(2022)).isFalse();
        assertThat(set.appliesTo(2026)).isFalse();
    }

    // --- addRule 테스트 ---

    @Test
    void 규칙_추가_시_양방향_관계가_설정된다() {
        RequirementSet set = createRequirementSet();
        GraduationRule rule = GraduationRule.create(1L, "전공 최소학점",
                "{\"minCredits\": 60}", "전공 최소 60학점 이수");

        set.addRule(rule);

        assertThat(set.getRules()).hasSize(1);
        assertThat(set.getRules().get(0)).isSameAs(rule);
        assertThat(rule.getRequirementSet()).isSameAs(set);
    }

    @Test
    void 여러_규칙을_추가할_수_있다() {
        RequirementSet set = createRequirementSet();
        GraduationRule rule1 = GraduationRule.create(1L, "전공 최소학점",
                "{\"minCredits\": 60}", null);
        GraduationRule rule2 = GraduationRule.create(2L, "교양 필수과목",
                "{\"courses\": [\"GEN1001\"]}", null);

        set.addRule(rule1);
        set.addRule(rule2);

        assertThat(set.getRules()).hasSize(2);
    }

    @Test
    void null_규칙을_추가하면_예외가_발생한다() {
        RequirementSet set = createRequirementSet();

        assertThatThrownBy(() -> set.addRule(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getRules는_불변_리스트를_반환한다() {
        RequirementSet set = createRequirementSet();
        GraduationRule rule = GraduationRule.create(1L, "전공 최소학점",
                "{\"minCredits\": 60}", null);
        set.addRule(rule);

        assertThatThrownBy(() -> set.getRules().add(
                GraduationRule.create(2L, "다른 규칙", "{}", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- GraduationRule 생성 테스트 ---

    @Test
    void GraduationRule_생성_시_필드가_올바르게_저장된다() {
        GraduationRule rule = GraduationRule.create(1L, "전공 최소학점",
                "{\"minCredits\": 60}", "전공 최소 60학점 이수");

        assertThat(rule.getRuleTypeId()).isEqualTo(1L);
        assertThat(rule.getRuleName()).isEqualTo("전공 최소학점");
        assertThat(rule.getRuleConfig()).isEqualTo("{\"minCredits\": 60}");
        assertThat(rule.getDescription()).isEqualTo("전공 최소 60학점 이수");
    }

    @Test
    void GraduationRule_생성_시_ruleTypeId가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> GraduationRule.create(null, "전공 최소학점",
                "{\"minCredits\": 60}", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RequirementSet createRequirementSet() {
        return RequirementSet.create(1L, 2023, 2025, 1,
                "컴퓨터·AI학부 23~25학번 졸업 요건", null);
    }
}
