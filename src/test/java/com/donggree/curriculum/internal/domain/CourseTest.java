package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CourseTest {

    @Test
    void 교과목_생성_시_교과목코드_교과목명_학점이_저장된다() {
        Course course = Course.create("CSE1001", "컴퓨터프로그래밍", 3);

        assertThat(course.getCourseCode()).isEqualTo("CSE1001");
        assertThat(course.getCourseName()).isEqualTo("컴퓨터프로그래밍");
        assertThat(course.getCredits()).isEqualTo(3);
    }

    @Test
    void 학점이_0_이하이면_예외가_발생한다() {
        assertThatThrownBy(() -> Course.create("CSE1001", "컴퓨터프로그래밍", 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Course.create("CSE1001", "컴퓨터프로그래밍", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
