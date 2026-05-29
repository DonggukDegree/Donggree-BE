package com.donggree.transcript;

/**
 * transcript 모듈 외부에 수강 이력 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * passed는 내부 Grade enum 노출 없이 이수 여부를 전달한다 (F·NP = false, 나머지 = true).
 */
public record CourseRecordView(
        String semester,
        String courseCode,
        String courseName,
        int credits,
        boolean passed,
        boolean retake
) {}
