package com.donggree.transcript.internal.application.command;

import com.donggree.transcript.internal.domain.enums.Grade;

/**
 * 수강 이력 생성/치환에 필요한 입력 커맨드. PDF 파싱 결과를 그대로 담는다.
 * courseTypeName은 PDF 원시 문자열이며, course_name·credits·areaName은 업로드 시점 스냅샷이다.
 */
public record CourseRecordCreateCommand(
        String semester,
        String courseTypeName,
        String areaName,
        String courseCode,
        String courseName,
        int credits,
        Grade grade,
        boolean retake) {}
