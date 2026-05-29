package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.RuleCategory;
import org.junit.jupiter.api.Test;

class RuleTypeTest {

    @Test
    void 규칙유형_생성_시_유형명과_카테고리와_설명이_저장된다() {
        RuleType ruleType = RuleType.create("TOTAL_CREDITS", RuleCategory.GRADUATION_REQ, "총 취득학점 규칙");

        assertThat(ruleType.getTypeName()).isEqualTo("TOTAL_CREDITS");
        assertThat(ruleType.getCategory()).isEqualTo(RuleCategory.GRADUATION_REQ);
        assertThat(ruleType.getDescription()).isEqualTo("총 취득학점 규칙");
    }

    @Test
    void 규칙유형_생성_시_설명은_없어도_된다() {
        RuleType ruleType = RuleType.create("MIN_AREA_CREDITS", RuleCategory.LIBERAL, null);

        assertThat(ruleType.getTypeName()).isEqualTo("MIN_AREA_CREDITS");
        assertThat(ruleType.getCategory()).isEqualTo(RuleCategory.LIBERAL);
        assertThat(ruleType.getDescription()).isNull();
    }

    @Test
    void category가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> RuleType.create("TOTAL_CREDITS", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
    }
}
