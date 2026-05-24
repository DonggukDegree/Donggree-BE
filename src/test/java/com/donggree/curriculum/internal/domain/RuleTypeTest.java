package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleTypeTest {

    @Test
    void 규칙유형_생성_시_유형명과_설명이_저장된다() {
        RuleType ruleType = RuleType.create("최소학점", "영역별 최소 이수 학점 규칙");

        assertThat(ruleType.getTypeName()).isEqualTo("최소학점");
        assertThat(ruleType.getDescription()).isEqualTo("영역별 최소 이수 학점 규칙");
    }

    @Test
    void 규칙유형_생성_시_설명은_없어도_된다() {
        RuleType ruleType = RuleType.create("최소학점", null);

        assertThat(ruleType.getTypeName()).isEqualTo("최소학점");
        assertThat(ruleType.getDescription()).isNull();
    }
}
