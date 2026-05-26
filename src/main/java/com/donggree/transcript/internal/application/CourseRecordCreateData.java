package com.donggree.transcript.internal.application;

import com.donggree.transcript.internal.domain.enums.CourseType;
import com.donggree.transcript.internal.domain.enums.Grade;

/**
 * 수강 이력 생성에 필요한 데이터. PDF 파싱 결과를 그대로 담는다.
 * course_name, credits는 업로드 시점 스냅샷으로 course_record에 직접 저장된다.
 */
public record CourseRecordCreateData(
        String semester,
        CourseType courseType,
        String areaName,
        String courseCode,
        String courseName,
        int credits,
        Grade grade,
        boolean retake
) {
}
