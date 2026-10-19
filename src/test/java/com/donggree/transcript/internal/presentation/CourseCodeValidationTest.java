package com.donggree.transcript.internal.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.transcript.internal.presentation.dto.CourseRecordUpdateRequest.CourseItem;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CourseCodeValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {"DEV11079", "7777700", "12345678", " iot4005 "})
    void 영문숫자_8자리까지_정규화하여_허용한다(String code) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var item = item(code);
            assertThat(factory.getValidator().validate(item)).isEmpty();
            assertThat(item.courseCode()).matches("[A-Z0-9]{1,8}");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SSC2008-02", "ABC123456", "한글", "AB C"})
    void 프론트를_우회한_잘못된_학수번호도_거부한다(String code) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(item(code))).isNotEmpty();
        }
    }

    private CourseItem item(String code) {
        return new CourseItem("2026-1", "일교", null, code, "과목", 0, "P", false);
    }
}
