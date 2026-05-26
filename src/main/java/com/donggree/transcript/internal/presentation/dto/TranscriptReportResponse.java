package com.donggree.transcript.internal.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/users/me/reports 응답 DTO.
 * 이 응답에서만 사용하는 중첩 타입은 내부 record로 정의한다.
 */
public record TranscriptReportResponse(
        Meta meta,
        List<SemesterCourses> courses,
        Long nextCursor,
        boolean hasNext
) {

    public record Meta(
            Long reportId,
            int admissionYear,
            String department,
            String subMajor1,
            String subMajor2,
            String dualMajor1,
            String dualMajor2,
            String academicStatus,
            int totalCredits,
            BigDecimal gpa,
            int completedSemesters
    ) {
    }

    public record SemesterCourses(String semester, List<CourseRecord> records) {
    }

    public record CourseRecord(
            Long id,
            String courseCode,
            String courseName,
            int credits,
            String courseType,
            String areaName,
            String grade,
            boolean retake
    ) {
    }
}
