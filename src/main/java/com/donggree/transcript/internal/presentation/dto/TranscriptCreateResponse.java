package com.donggree.transcript.internal.presentation.dto;

import java.util.List;

/**
 * PUT /api/users/me/reports 응답 DTO.
 * 등록된 성적표의 총취득학점과 파싱된 취득학점 합, 그 차이(creditGap), F 평가방식 확인 대상을 담는다.
 * creditGap이 0이 아니면 PDF 파싱 결과와 총취득학점이 불일치함을 의미하며,
 * 프론트엔드는 이를 보고 사용자에게 수강 이력 추가를 안내할 수 있다.
 * (양수: 취득학점 누락, 음수: 취득학점 초과)
 */
public record TranscriptCreateResponse(
        int totalCredits, int recordedCredits, int creditGap, List<GradeReviewCourse> gradeReviewCourses) {
    public record GradeReviewCourse(String semester, String courseCode, String courseName) {}
}
