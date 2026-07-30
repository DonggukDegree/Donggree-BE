package com.donggree.curriculum.internal.domain.ruletype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.CourseType;
import org.junit.jupiter.api.Test;

class RuleTypeTest {

    @Test
    void 규칙유형_생성_시_유형명과_courseType과_설명이_저장된다() {
        RuleType ruleType = RuleType.create("TOTAL_CREDITS", null, "총 취득학점 규칙");

        assertThat(ruleType.getTypeName()).isEqualTo("TOTAL_CREDITS");
        assertThat(ruleType.getCourseType()).isNull();
        assertThat(ruleType.getDescription()).isEqualTo("총 취득학점 규칙");
    }

    @Test
    void 규칙유형_생성_시_설명은_없어도_된다() {
        RuleType ruleType = RuleType.create("MIN_AREA_CREDITS", CourseType.LIBERAL_ARTS, null);

        assertThat(ruleType.getTypeName()).isEqualTo("MIN_AREA_CREDITS");
        assertThat(ruleType.getCourseType()).isEqualTo(CourseType.LIBERAL_ARTS);
        assertThat(ruleType.getDescription()).isNull();
    }

    @Test
    void courseType이_null이면_졸업요건_규칙으로_생성된다() {
        RuleType ruleType = RuleType.create("TOTAL_CREDITS", null, null);

        assertThat(ruleType.getCourseType()).isNull();
    }

    @Test
    void typeName이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> RuleType.create(null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeName");
    }

    @Test
    void typeName이_빈_문자열이면_예외가_발생한다() {
        assertThatThrownBy(() -> RuleType.create("  ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeName");
    }
}
