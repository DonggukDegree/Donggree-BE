package com.donggree.transcript.internal.domain;

import com.donggree.transcript.internal.domain.enums.Grade;

/**
 * 수강 이력 치환 시 전달되는 도메인 파라미터 객체.
 * 한 건의 수강 이력 값을 담아 {@link Transcript#replaceCourseRecords}에 전달한다.
 */
public record CourseRecordData(
        String semester,
        String courseTypeName,
        String areaName,
        String courseCode,
        String courseName,
        int credits,
        Grade grade,
        boolean retake) {}
