package com.donggree.graduation.internal.domain.evaluation;
/** 판정과 이수 현황 조립에 필요한 수강 사실. 다른 모듈의 DTO를 보관하지 않는다. */
public record EvaluationCourse(
        String semester,
        String courseCode,
        String courseTypeName,
        String pdfAreaName,
        String courseName,
        int credits,
        boolean passed) {}
