package com.donggree.user.internal.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @NotBlank(message = "학번은 필수입니다.")
        @Size(max = 10, message = "학번은 10자 이하여야 합니다.")
        @Schema(example = "2023123456")
        String studentId,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 5, message = "이름은 5자 이하여야 합니다.")
        @Schema(example = "동그리")
        String name
) {
}
