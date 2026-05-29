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
        assertThatThrownBy(() -> Course.create("CSE1001", "컴퓨터프로그래밍", 0)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Course.create("CSE1001", "컴퓨터프로그래밍", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 동치과목_그룹_지정_후_equivalentCourseId가_저장된다() {
        Course course = Course.create("AWS2019", "기초프로그래밍", 3);

        course.assignEquivalentCourse(5L);

        assertThat(course.getEquivalentCourseId()).isEqualTo(5L);
    }

    @Test
    void 동치과목_그룹_미지정_시_equivalentCourseId는_null이다() {
        Course course = Course.create("CSE1001", "컴퓨터프로그래밍", 3);

        assertThat(course.getEquivalentCourseId()).isNull();
    }

    @Test
    void equivalentCourseId가_0_이하이면_예외가_발생한다() {
        Course course = Course.create("CSE1001", "컴퓨터프로그래밍", 3);

        assertThatThrownBy(() -> course.assignEquivalentCourse(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equivalentCourseId");

        assertThatThrownBy(() -> course.assignEquivalentCourse(-1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equivalentCourseId에_null을_지정하면_그룹_해제된다() {
        Course course = Course.create("CSE1001", "컴퓨터프로그래밍", 3);
        course.assignEquivalentCourse(5L);

        course.assignEquivalentCourse(null);

        assertThat(course.getEquivalentCourseId()).isNull();
    }
}
