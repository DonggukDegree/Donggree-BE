package com.donggree.curriculum.internal.domain.areatype;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AreaTypeTest {

    @Test
    void 이수영역_생성_시_영역명이_저장된다() {
        AreaType areaType = AreaType.create("자아");

        assertThat(areaType.getAreaName()).isEqualTo("자아");
    }
}
