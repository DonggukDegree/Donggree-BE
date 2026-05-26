package com.donggree.transcript.internal.presentation.dto;

/**
 * 개별 수강 이력 응답 DTO.
 */
public record CourseRecordResponse(
        Long id,
        String courseCode,
        String courseName,
        int credits,
        String courseType,
        String area,
        String grade,
        boolean retake
) {
}
