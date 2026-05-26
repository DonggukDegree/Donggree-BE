package com.donggree.transcript.internal.presentation.dto;

import java.util.List;

/**
 * 한 학기의 수강 이력 그룹 응답 DTO.
 */
public record SemesterCoursesResponse(
        String semester,
        List<CourseRecordResponse> records
) {
}
