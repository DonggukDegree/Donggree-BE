package com.donggree.transcript.internal.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * PATCH /api/users/me/reports 요청 DTO.
 * 수동으로 추가할 수강 이력 목록을 받는다.
 */
public record CourseRecordAddRequest(@NotEmpty List<@Valid CourseItem> courses) {

    public record CourseItem(
            @NotBlank String semester,
            @NotBlank String courseType,
            String areaName,
            @NotBlank String courseCode,
            @NotBlank String courseName,
            @Positive int credits,
            @NotBlank String grade,
            boolean retake) {}
}
