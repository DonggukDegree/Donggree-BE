package com.donggree.transcript.internal.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * PATCH /api/users/me/reports 요청 DTO.
 * 수정·삭제·추가를 모두 반영하기 위해 변경 후의 전체 수강 이력 목록을 받는다.
 * 전송된 목록이 곧 최종 상태이며, 기존 이력은 이 목록으로 통째 치환된다.
 */
public record CourseRecordUpdateRequest(@NotEmpty List<@Valid CourseItem> courses) {

    public record CourseItem(
            @NotBlank String semester,
            @NotBlank String courseType,
            String areaName,
            @NotBlank String courseCode,
            @NotBlank String courseName,
            @NotNull @PositiveOrZero Integer credits,
            @NotBlank String grade,
            boolean retake) {}
}
