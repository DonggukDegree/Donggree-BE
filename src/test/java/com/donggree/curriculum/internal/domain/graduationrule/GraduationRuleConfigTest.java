package com.donggree.curriculum.internal.domain.graduationrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GraduationRuleConfigTest {

    @Test
    void 객체_키_공백_숫자_표현과_역할_선택_순서는_중복_기준에_영향이_없다() {
        String first =
                "{\"nested\":{\"z\":2,\"a\":1},\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SINGLE_PRIMARY\"],\"minCredits\":36.0}";
        String second =
                "{ \"minCredits\":3.6e1, \"applicableMajorRoles\":[\"DUAL_PRIMARY\",\"SINGLE_PRIMARY\"],\"nested\":{\"a\":1.0,\"z\":2} }";
        assertThat(GraduationRuleConfig.normalize(first)).isEqualTo(GraduationRuleConfig.normalize(second));
    }

    @Test
    void 역할_값과_그밖의_옵션_차이는_보존한다() {
        assertThat(GraduationRuleConfig.normalize("{\"applicableMajorRoles\":[\"SECONDARY\"]}"))
                .isNotEqualTo(GraduationRuleConfig.normalize("{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}"));
        assertThat(GraduationRuleConfig.normalize("{\"minCredits\":36}"))
                .isNotEqualTo(GraduationRuleConfig.normalize("{\"minCredits\":72}"));
    }

    @Test
    void 다른_배열의_순서와_null_누락은_임의로_동일시하지_않는다() {
        assertThat(GraduationRuleConfig.normalize("{\"courseCodes\":[\"B\",\"A\"]}"))
                .isEqualTo("{\"courseCodes\":[\"B\",\"A\"]}");
        assertThat(GraduationRuleConfig.normalize("{\"courseType\":null}")).isNotEqualTo("{}");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "{", "[]", "null", "123", "{} {}"})
    void 올바른_JSON_객체만_허용한다(String config) {
        assertThatThrownBy(() -> GraduationRuleConfig.normalize(config)).isInstanceOf(IllegalArgumentException.class);
    }
}
