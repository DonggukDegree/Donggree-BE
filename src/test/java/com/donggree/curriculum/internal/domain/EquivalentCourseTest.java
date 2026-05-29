package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EquivalentCourseTest {

    @Test
    void 동치과목_그룹_생성_시_이름이_저장된다() {
        EquivalentCourse group = EquivalentCourse.create("기초프로그래밍계열");

        assertThat(group.getName()).isEqualTo("기초프로그래밍계열");
        assertThat(group.getId()).isNull();
    }
}
