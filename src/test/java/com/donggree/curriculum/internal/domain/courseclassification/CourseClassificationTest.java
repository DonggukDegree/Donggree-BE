package com.donggree.curriculum.internal.domain.courseclassification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.CourseType;
import org.junit.jupiter.api.Test;

class CourseClassificationTest {

    @Test
    void 과목분류_생성_시_모든_필드가_저장된다() {
        CourseClassification cc = CourseClassification.create(
                "CS001", 2023, 2025, CourseType.ACADEMIC_FOUNDATION, 10L, "개론", "물리", "물리학개론");

        assertThat(cc.getCourseCode()).isEqualTo("CS001");
        assertThat(cc.getTag()).isEqualTo("물리학개론");
        assertThat(cc.getStudentYearStart()).isEqualTo(2023);
        assertThat(cc.getStudentYearEnd()).isEqualTo(2025);
        assertThat(cc.getCourseType()).isEqualTo(CourseType.ACADEMIC_FOUNDATION);
        assertThat(cc.getAreaTypeId()).isEqualTo(10L);
        assertThat(cc.getSubCategory()).isEqualTo("개론");
        assertThat(cc.getSubjectDomain()).isEqualTo("물리");
    }

    @Test
    void areaTypeId와_subCategory와_subjectDomain과_tag가_null이어도_생성된다() {
        CourseClassification cc =
                CourseClassification.create("CS001", 2023, 2025, CourseType.FIRST_MAJOR, null, null, null, null);

        assertThat(cc.getAreaTypeId()).isNull();
        assertThat(cc.getSubCategory()).isNull();
        assertThat(cc.getSubjectDomain()).isNull();
        assertThat(cc.getTag()).isNull();
    }

    @Test
    void courseCode가_null이면_예외가_발생한다() {
        assertThatThrownBy(() ->
                        CourseClassification.create(null, 2023, 2025, CourseType.FIRST_MAJOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("courseCode");
    }

    @Test
    void courseType이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> CourseClassification.create("CS001", 2023, 2025, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("courseType");
    }

    @Test
    void yearStart가_yearEnd보다_크면_예외가_발생한다() {
        assertThatThrownBy(() -> CourseClassification.create(
                        "CS001", 2025, 2023, CourseType.FIRST_MAJOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("studentYearStart");
    }

    @Test
    void studentYearStart가_0_이하이면_예외가_발생한다() {
        assertThatThrownBy(() ->
                        CourseClassification.create("CS001", 0, 2023, CourseType.FIRST_MAJOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void studentYearEnd가_음수이면_예외가_발생한다() {
        assertThatThrownBy(() ->
                        CourseClassification.create("CS001", 2023, -1, CourseType.FIRST_MAJOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void yearStart와_yearEnd가_같으면_단일_연도_분류로_생성된다() {
        CourseClassification cc =
                CourseClassification.create("CS001", 2023, 2023, CourseType.LIBERAL_ARTS, null, null, null, null);

        assertThat(cc.getStudentYearStart()).isEqualTo(2023);
        assertThat(cc.getStudentYearEnd()).isEqualTo(2023);
    }

    // --- update 테스트 ---

    @Test
    void 수정_시_모든_필드가_전체_교체된다() {
        CourseClassification cc =
                CourseClassification.create("CS001", 2023, 2025, CourseType.FIRST_MAJOR, 10L, "개론", "물리", "옛이름");

        cc.update("CS002", 2024, 2026, CourseType.SECOND_MAJOR, 20L, "실험", "화학", "새이름");

        assertThat(cc.getCourseCode()).isEqualTo("CS002");
        assertThat(cc.getStudentYearStart()).isEqualTo(2024);
        assertThat(cc.getStudentYearEnd()).isEqualTo(2026);
        assertThat(cc.getCourseType()).isEqualTo(CourseType.SECOND_MAJOR);
        assertThat(cc.getAreaTypeId()).isEqualTo(20L);
        assertThat(cc.getSubCategory()).isEqualTo("실험");
        assertThat(cc.getSubjectDomain()).isEqualTo("화학");
        assertThat(cc.getTag()).isEqualTo("새이름");
    }

    @Test
    void 수정_시에도_불변식이_검증된다() {
        CourseClassification cc =
                CourseClassification.create("CS001", 2023, 2025, CourseType.FIRST_MAJOR, null, null, null, null);

        assertThatThrownBy(() -> cc.update("CS001", 2026, 2024, CourseType.FIRST_MAJOR, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("studentYearStart");
    }
}
