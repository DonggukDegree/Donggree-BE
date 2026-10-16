package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import org.junit.jupiter.api.Test;

class CourseClassificationInferenceTest {

    @Test
    void 이수구분_전공은_제1전공으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("전공").courseType())
                .isEqualTo(CourseType.FIRST_MAJOR);
    }

    @Test
    void 이수구분_전필도_제1전공으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("전필").courseType())
                .isEqualTo(CourseType.FIRST_MAJOR);
    }

    @Test
    void 교직선택과_교직필수는_제1전공의_별도_영역으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("직선"))
                .isEqualTo(new CourseClassificationView(CourseType.FIRST_MAJOR, "교직선택", null, null));
        assertThat(GraduationQueryService.inferClassification("직필"))
                .isEqualTo(new CourseClassificationView(CourseType.FIRST_MAJOR, "교직필수", null, null));
    }

    @Test
    void 이수구분_복수는_제2전공으로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("복수1").courseType())
                .isEqualTo(CourseType.SECOND_MAJOR);
        assertThat(GraduationQueryService.inferClassification("복수2").courseType())
                .isEqualTo(CourseType.SECOND_MAJOR);
    }

    @Test
    void 알_수_없는_이수구분은_courseType이_null로_추론된다() {
        assertThat(GraduationQueryService.inferClassification("알수없음").courseType())
                .isNull();
    }

    @Test
    void 이수구분이_null이면_courseType이_null로_추론된다() {
        assertThat(GraduationQueryService.inferClassification(null).courseType())
                .isNull();
    }
}
