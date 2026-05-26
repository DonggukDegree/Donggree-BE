package com.donggree.transcript.internal.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 학업 정보 조회 응답 DTO.
 * 상단 메타 정보와 학기별 수강 이력(커서 페이지네이션)을 포함한다.
 */
public record TranscriptReportResponse(
        Meta meta,
        List<SemesterCoursesResponse> courses,
        Long nextCursor,
        boolean hasNext
) {

    /**
     * 성적표 상단에 표시되는 학업 메타 정보.
     */
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
}
