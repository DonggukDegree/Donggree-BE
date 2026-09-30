package com.donggree.transcript.internal.application.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 학업 정보 조회(읽기) 프로젝션.
 * course_name, credits, area_name은 course_record에 반정규화되어 있으므로
 * 컨트롤러에서 별도 curriculum 조회 없이 바로 응답 DTO로 변환 가능하다.
 * 학과·단과대 이름은 컨트롤러에서 조회하되, 학과 연결 실패 시 PDF 원문 이름으로 대체한다.
 */
public record TranscriptReportProjection(RawMeta meta, List<RawSemesterGroup> semesterGroups) {

    public record RawMeta(
            int admissionYear,
            Long departmentId,
            Long subMajor1Id,
            Long subMajor2Id,
            Long dualMajor1Id,
            Long dualMajor2Id,
            String pdfDepartment,
            String pdfCollegeName,
            String academicStatus,
            int totalCredits,
            BigDecimal gpa,
            BigDecimal majorGpa,
            BigDecimal dualMajor1Gpa,
            int completedSemesters,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record RawSemesterGroup(String semester, List<RawCourseRecord> records) {}

    public record RawCourseRecord(
            Long id,
            String courseCode,
            String courseName,
            int credits,
            String areaName,
            String courseType,
            String grade,
            boolean retake) {}
}
