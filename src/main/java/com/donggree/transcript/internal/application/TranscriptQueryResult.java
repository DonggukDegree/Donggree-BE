package com.donggree.transcript.internal.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * 학업 정보 조회 서비스 계층 반환 객체.
 * course_name, credits, area_name은 course_record에 반정규화되어 있으므로
 * 컨트롤러에서 별도 curriculum 조회 없이 바로 응답 DTO로 변환 가능하다.
 */
public record TranscriptQueryResult(
        RawMeta meta,
        List<RawSemesterGroup> semesterGroups,
        Long nextCursor,
        boolean hasNext
) {

    public record RawMeta(
            Long reportId,
            int admissionYear,
            Long departmentId,
            Long subMajor1Id,
            Long subMajor2Id,
            Long dualMajor1Id,
            Long dualMajor2Id,
            String academicStatus,
            int totalCredits,
            BigDecimal gpa,
            int completedSemesters
    ) {
    }

    public record RawSemesterGroup(String semester, List<RawCourseRecord> records) {
    }

    public record RawCourseRecord(
            Long id,
            String courseCode,
            String courseName,
            int credits,
            String areaName,
            String courseType,
            String grade,
            boolean retake
    ) {
    }
}
