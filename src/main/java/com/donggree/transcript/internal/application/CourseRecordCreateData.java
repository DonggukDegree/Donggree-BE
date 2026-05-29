package com.donggree.transcript.internal.application;

import com.donggree.transcript.internal.domain.enums.Grade;

/**
 * 수강 이력 생성에 필요한 데이터. PDF 파싱 결과를 그대로 담는다.
 * courseTypeName은 PDF 원시 문자열이며, course_name·credits·areaName은 업로드 시점 스냅샷이다.
 */
public record CourseRecordCreateData(
        String semester,
        String courseTypeName,
        String areaName,
        String courseCode,
        String courseName,
        int credits,
        Grade grade,
        boolean retake
) {
}
