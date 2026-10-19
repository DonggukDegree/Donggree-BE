package com.donggree.transcript.internal.application.command;

import java.util.List;

/**
 * PDF 파싱 결과를 담는 내부 전달 객체.
 * 컨트롤러에서 커리큘럼 ID 조회 후 createTranscript()에 전달할 때 사용한다.
 */
public record TranscriptParseResult(
        ParsedTranscriptData parsedData, String rawDataJson, List<GradeReviewCourse> gradeReviewCourses) {
    public TranscriptParseResult(ParsedTranscriptData parsedData, String rawDataJson) {
        this(parsedData, rawDataJson, List.of());
    }

    public record GradeReviewCourse(String semester, String courseCode, String courseName) {}
}
