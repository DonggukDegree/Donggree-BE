package com.donggree.transcript.internal.presentation.dto;

import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse.SemesterCourses;
import java.math.BigDecimal;
import java.util.List;

/**
 * PATCH /api/users/me/reports 응답 DTO.
 * 치환 후 재계산된 총취득학점·평점 평균과, 학기 오름차순으로 그룹핑한 전체 수강 이력을 반환한다.
 * 수강 이력 구조는 조회 응답과 동일하므로 {@link SemesterCourses}를 재사용한다.
 */
public record CourseRecordUpdateResponse(int totalCredits, BigDecimal gpa, List<SemesterCourses> courses) {}
