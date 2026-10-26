package com.donggree.place.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceNameTest {

    @Test
    void 공백과_특수문자를_지우고_영문은_소문자로_맞춘다() {
        assertThat(PlaceName.normalize(" Cafe 동국! ")).isEqualTo("cafe동국");
        assertThat(PlaceName.normalize("cafe동국")).isEqualTo("cafe동국");
        assertThat(PlaceName.normalize("동국반점 2호점")).isEqualTo("동국반점2호점");
    }

    @Test
    void 문자와_숫자가_없으면_빈_문자열이다() {
        assertThat(PlaceName.normalize("★ ~")).isEmpty();
        assertThat(PlaceName.normalize(null)).isEmpty();
    }
}
