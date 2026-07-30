package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.command.CourseClassificationCommand;
import com.donggree.curriculum.internal.application.command.CourseClassificationUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.areatype.AreaType;
import com.donggree.curriculum.internal.domain.areatype.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassification;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassificationRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class CourseClassificationCommandServiceTest {

    private final CourseClassificationRepository courseClassificationRepository =
            Mockito.mock(CourseClassificationRepository.class);
    private final AreaTypeRepository areaTypeRepository = Mockito.mock(AreaTypeRepository.class);
    private final CourseClassificationCommandService service =
            new CourseClassificationCommandService(courseClassificationRepository, areaTypeRepository);

    private CourseClassification classification(Long id, String code, Long areaTypeId, String tag) {
        CourseClassification cc =
                CourseClassification.create(code, 2023, 2025, CourseType.FIRST_MAJOR, areaTypeId, null, null, tag);
        if (id != null) {
            ReflectionTestUtils.setField(cc, "id", id);
        }
        return cc;
    }

    private AreaType areaType(Long id, String name) {
        AreaType area = AreaType.create(name);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    private CourseClassificationUpsertCommand upsertItem(Long id, CourseClassificationCommand data) {
        return new CourseClassificationUpsertCommand(id, data);
    }

    @Test
    void 배치_업서트_시_id가_null이면_등록하고_생성된_ID를_반환한다() {
        CourseClassificationCommand data =
                new CourseClassificationCommand("CS001", "자료구조", 2023, 2025, CourseType.FIRST_MAJOR, 10L, null, null);
        given(areaTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(areaType(10L, "전공기초")));
        given(courseClassificationRepository.findByCourseCodeAndStudentYearStartAndStudentYearEnd("CS001", 2023, 2025))
                .willReturn(Optional.empty());
        given(courseClassificationRepository.save(any())).willAnswer(invocation -> {
            CourseClassification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        List<Long> ids = service.upsert(List.of(upsertItem(null, data)));

        assertThat(ids).containsExactly(100L);
    }

    @Test
    void 배치_업서트_시_id가_있으면_수정하고_도메인이_전체_교체된다() {
        CourseClassification existing = classification(1L, "CS001", 10L, "옛이름");
        given(courseClassificationRepository.findById(1L)).willReturn(Optional.of(existing));
        given(areaTypeRepository.findAllById(Set.of(20L))).willReturn(List.of(areaType(20L, "기본소양")));
        given(courseClassificationRepository.findByCourseCodeAndStudentYearStartAndStudentYearEnd("CS002", 2024, 2026))
                .willReturn(Optional.empty());

        List<Long> ids = service.upsert(List.of(upsertItem(
                1L,
                new CourseClassificationCommand(
                        "CS002", "새이름", 2024, 2026, CourseType.SECOND_MAJOR, 20L, "실험", "화학"))));

        assertThat(ids).containsExactly(1L);
        assertThat(existing.getCourseCode()).isEqualTo("CS002");
        assertThat(existing.getTag()).isEqualTo("새이름");
        assertThat(existing.getCourseType()).isEqualTo(CourseType.SECOND_MAJOR);
    }

    @Test
    void 배치_업서트_등록_시_기존과_동일_코드_연도범위가_있으면_예외를_던진다() {
        CourseClassificationCommand data =
                new CourseClassificationCommand("CS001", null, 2023, 2025, CourseType.FIRST_MAJOR, null, null, null);
        given(courseClassificationRepository.findByCourseCodeAndStudentYearStartAndStudentYearEnd("CS001", 2023, 2025))
                .willReturn(Optional.of(classification(1L, "CS001", null, null)));

        assertThatThrownBy(() -> service.upsert(List.of(upsertItem(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DUPLICATE_COURSE_CLASSIFICATION));
    }

    @Test
    void 배치_업서트_등록_시_존재하지_않는_areaTypeId면_예외를_던진다() {
        CourseClassificationCommand data =
                new CourseClassificationCommand("CS001", null, 2023, 2025, CourseType.FIRST_MAJOR, 999L, null, null);
        given(areaTypeRepository.findAllById(Set.of(999L))).willReturn(List.of());

        assertThatThrownBy(() -> service.upsert(List.of(upsertItem(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.AREA_TYPE_NOT_FOUND));
    }

    @Test
    void 배치_업서트_수정_시_존재하지_않는_분류면_예외를_던진다() {
        CourseClassificationCommand data =
                new CourseClassificationCommand("CS001", null, 2023, 2025, CourseType.FIRST_MAJOR, null, null, null);
        given(courseClassificationRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(List.of(upsertItem(404L, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.COURSE_CLASSIFICATION_NOT_FOUND));
    }

    @Test
    void 배치_안에_동일_코드_연도범위_항목이_중복되면_예외를_던진다() {
        CourseClassificationCommand data =
                new CourseClassificationCommand("CS001", null, 2023, 2025, CourseType.FIRST_MAJOR, null, null, null);

        assertThatThrownBy(() -> service.upsert(List.of(upsertItem(null, data), upsertItem(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DUPLICATE_COURSE_CLASSIFICATION));
    }
}
