package com.donggree.curriculum.internal.domain.requirementset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementSetTest {

    @Test
    void 졸업요건세트_생성_시_필드가_올바르게_저장된다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.getDepartmentId()).isEqualTo(1L);
        assertThat(set.getYearStart()).isEqualTo(2023);
        assertThat(set.getYearEnd()).isEqualTo(2025);
        assertThat(set.getTrack()).isEqualTo(RequirementTrack.ALL);
        assertThat(set.getVersion()).isEqualTo(1);
        assertThat(set.getDescription()).isEqualTo("컴퓨터·AI학부 23~25학번 졸업 요건");
        assertThat(set.isActive()).isTrue();
        assertThat(set.getRules()).isEmpty();
    }

    @Test
    void departmentId가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(null, 2023, 2025, RequirementTrack.ALL, 1, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void track이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(1L, 2023, 2025, null, 1, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void yearStart가_yearEnd보다_크면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(1L, 2025, 2023, RequirementTrack.ALL, 1, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void version이_0_이하이면_예외가_발생한다() {
        assertThatThrownBy(() -> RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, 0, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, -1, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- appliesTo 테스트 ---

    @Test
    void 입학년도가_범위_내이면_appliesTo는_true를_반환한다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.appliesTo(2023, RequirementTrack.GENERAL)).isTrue();
        assertThat(set.appliesTo(2024, RequirementTrack.GENERAL)).isTrue();
        assertThat(set.appliesTo(2025, RequirementTrack.GENERAL)).isTrue();
    }

    @Test
    void 입학년도가_범위_밖이면_appliesTo는_false를_반환한다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.appliesTo(2022, RequirementTrack.GENERAL)).isFalse();
        assertThat(set.appliesTo(2026, RequirementTrack.GENERAL)).isFalse();
    }

    @Test
    void ALL_세트는_일반과정_학생과_심화과정_학생_모두에게_적용된다() {
        RequirementSet set = createRequirementSet();

        assertThat(set.appliesTo(2024, RequirementTrack.GENERAL)).isTrue();
        assertThat(set.appliesTo(2024, RequirementTrack.ADVANCED)).isTrue();
    }

    @Test
    void 일반과정_세트는_심화과정_학생에게_적용되지_않는다() {
        RequirementSet set = RequirementSet.create(1L, 2023, 2025, RequirementTrack.GENERAL, 1, null, null, true);

        assertThat(set.appliesTo(2024, RequirementTrack.GENERAL)).isTrue();
        assertThat(set.appliesTo(2024, RequirementTrack.ADVANCED)).isFalse();
    }

    @Test
    void 심화과정_세트는_일반과정_학생에게_적용되지_않는다() {
        RequirementSet set = RequirementSet.create(1L, 2023, 2025, RequirementTrack.ADVANCED, 1, null, null, true);

        assertThat(set.appliesTo(2024, RequirementTrack.ADVANCED)).isTrue();
        assertThat(set.appliesTo(2024, RequirementTrack.GENERAL)).isFalse();
    }

    @Test
    void 과정이_맞아도_입학년도가_범위_밖이면_적용되지_않는다() {
        RequirementSet set = RequirementSet.create(1L, 2023, 2025, RequirementTrack.ADVANCED, 1, null, null, true);

        assertThat(set.appliesTo(2026, RequirementTrack.ADVANCED)).isFalse();
    }

    // --- addRule 테스트 ---

    @Test
    void 규칙_추가_시_요건세트에_포함된다() {
        RequirementSet set = createRequirementSet();
        GraduationRule rule = GraduationRule.create(1L, "전공 최소학점", "{\"minCredits\": 60}", "전공 최소 60학점 이수");

        set.addRule(rule);

        assertThat(set.getRules()).hasSize(1);
        assertThat(set.getRules().get(0)).isSameAs(rule);
    }

    @Test
    void 여러_규칙을_추가할_수_있다() {
        RequirementSet set = createRequirementSet();

        set.addRule(GraduationRule.create(1L, "전공 최소학점", "{\"minCredits\": 60}", null));
        set.addRule(GraduationRule.create(2L, "교양 필수과목", "{\"courseCodes\": [\"GEN1001\"]}", null));

        assertThat(set.getRules()).hasSize(2);
    }

    @Test
    void 동일한_규칙을_여러_요건세트에_연결할_수_있다() {
        RequirementSet setA = createRequirementSet();
        RequirementSet setB = RequirementSet.create(2L, 2023, 2025, RequirementTrack.ALL, 1, null, null, true);
        GraduationRule sharedRule = GraduationRule.create(1L, "총 학점", "{\"minCredits\": 130}", null);

        setA.addRule(sharedRule);
        setB.addRule(sharedRule);

        assertThat(setA.getRules()).contains(sharedRule);
        assertThat(setB.getRules()).contains(sharedRule);
    }

    @Test
    void getRules는_불변_리스트를_반환한다() {
        RequirementSet set = createRequirementSet();
        set.addRule(GraduationRule.create(1L, "전공 최소학점", "{\"minCredits\": 60}", null));

        assertThatThrownBy(() -> set.getRules().add(GraduationRule.create(2L, "다른 규칙", "{}", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- GraduationRule 생성 테스트 ---

    @Test
    void GraduationRule_생성_시_필드가_올바르게_저장된다() {
        GraduationRule rule = GraduationRule.create(1L, "전공 최소학점", "{\"minCredits\": 60}", "전공 최소 60학점 이수");

        assertThat(rule.getRuleTypeId()).isEqualTo(1L);
        assertThat(rule.getRuleName()).isEqualTo("전공 최소학점");
        assertThat(rule.getRuleConfig()).isEqualTo("{\"minCredits\": 60}");
        assertThat(rule.getDescription()).isEqualTo("전공 최소 60학점 이수");
    }

    @Test
    void GraduationRule_생성_시_ruleTypeId가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> GraduationRule.create(null, "전공 최소학점", "{\"minCredits\": 60}", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- update / replaceRules 테스트 ---

    @Test
    void 수정_시_식별정보를_포함한_모든_스칼라가_교체된다() {
        RequirementSet set = createRequirementSet();

        set.update(2L, 2024, 2026, RequirementTrack.ADVANCED, 2, "수정된 설명", "https://img/sheet.png", false);

        assertThat(set.getDepartmentId()).isEqualTo(2L);
        assertThat(set.getYearStart()).isEqualTo(2024);
        assertThat(set.getYearEnd()).isEqualTo(2026);
        assertThat(set.getTrack()).isEqualTo(RequirementTrack.ADVANCED);
        assertThat(set.getVersion()).isEqualTo(2);
        assertThat(set.getDescription()).isEqualTo("수정된 설명");
        assertThat(set.getSheetImageUrl()).isEqualTo("https://img/sheet.png");
        assertThat(set.isActive()).isFalse();
    }

    @Test
    void 수정_시에도_불변식이_검증된다() {
        RequirementSet set = createRequirementSet();

        assertThatThrownBy(() -> set.update(1L, 2026, 2024, RequirementTrack.ALL, 1, null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 규칙_목록을_통째로_교체한다() {
        RequirementSet set = createRequirementSet();
        set.addRule(GraduationRule.create(1L, "옛 규칙", "{}", null));

        GraduationRule a = GraduationRule.create(2L, "새 규칙 A", "{}", null);
        GraduationRule b = GraduationRule.create(3L, "새 규칙 B", "{}", null);
        set.replaceRules(List.of(a, b));

        assertThat(set.getRules()).containsExactly(a, b);
    }

    @Test
    void 빈_목록으로_교체하면_모든_규칙이_해제된다() {
        RequirementSet set = createRequirementSet();
        set.addRule(GraduationRule.create(1L, "규칙", "{}", null));

        set.replaceRules(List.of());

        assertThat(set.getRules()).isEmpty();
    }

    private RequirementSet createRequirementSet() {
        return RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, 1, "컴퓨터·AI학부 23~25학번 졸업 요건", null, true);
    }
}
