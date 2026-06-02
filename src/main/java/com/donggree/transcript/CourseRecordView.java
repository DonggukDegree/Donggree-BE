package com.donggree.transcript;

/**
 * transcript 모듈 외부에 수강 이력 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * passed는 내부 Grade enum 노출 없이 이수 여부를 전달한다 (F·NP = false, 나머지 = true).
 * courseTypeName은 PDF 원시값(공교, 학기, 전공, 일교 등) — course_classification 미등록 과목의 courseType 추론에 사용한다.
 * pdfAreaName은 PDF 원시값(기초, 전문, 동국 등) — course_classification 미등록 과목의 areaName 표시 fallback에 사용한다.
 */
public record CourseRecordView(
        String semester,
        String courseCode,
        String courseTypeName,
        String pdfAreaName,
        String courseName,
        int credits,
        boolean passed,
        boolean retake) {}
