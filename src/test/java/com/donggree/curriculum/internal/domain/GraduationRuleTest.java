package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GraduationRuleTest {

    @Test
    void 졸업_규칙_생성_시_모든_필드가_저장된다() {
        GraduationRule rule = GraduationRule.create(1L, "총학점 130 이상", "{\"min\":130}", "총 취득학점 요건");

        assertThat(rule.getRuleTypeId()).isEqualTo(1L);
        assertThat(rule.getRuleName()).isEqualTo("총학점 130 이상");
        assertThat(rule.getRuleConfig()).isEqualTo("{\"min\":130}");
        assertThat(rule.getDescription()).isEqualTo("총 취득학점 요건");
    }

    @Test
    void ruleTypeId가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> GraduationRule.create(null, "이름", "{}", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleTypeId");
    }

    @Test
    void ruleName이_비어있으면_예외가_발생한다() {
        assertThatThrownBy(() -> GraduationRule.create(1L, " ", "{}", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleName");
    }

    @Test
    void ruleConfig가_비어있으면_예외가_발생한다() {
        assertThatThrownBy(() -> GraduationRule.create(1L, "이름", " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleConfig");
    }

    @Test
    void 수정_시_모든_필드가_전체_교체된다() {
        GraduationRule rule = GraduationRule.create(1L, "옛이름", "{\"min\":130}", "옛 설명");

        rule.update(2L, "새이름", "{\"min\":140}", "새 설명");

        assertThat(rule.getRuleTypeId()).isEqualTo(2L);
        assertThat(rule.getRuleName()).isEqualTo("새이름");
        assertThat(rule.getRuleConfig()).isEqualTo("{\"min\":140}");
        assertThat(rule.getDescription()).isEqualTo("새 설명");
    }

    @Test
    void 수정_시에도_불변식이_검증된다() {
        GraduationRule rule = GraduationRule.create(1L, "이름", "{}", null);

        assertThatThrownBy(() -> rule.update(null, "이름", "{}", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleTypeId");
    }
}
