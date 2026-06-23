package com.donggree.curriculum.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 커리큘럼(졸업 요건 데이터) 관련 에러 코드.
 */
@Getter
@AllArgsConstructor
public enum CurriculumErrorCode implements BaseErrorCode {
    COURSE_CLASSIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM404_1", "존재하지 않는 과목 분류입니다."),

    AREA_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "CURRICULUM400_1", "존재하지 않는 이수 영역입니다."),

    DUPLICATE_COURSE_CLASSIFICATION(HttpStatus.CONFLICT, "CURRICULUM409_1", "동일한 과목코드와 적용 입학년도 범위의 분류가 이미 존재합니다."),

    GRADUATION_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM404_2", "존재하지 않는 졸업 규칙입니다."),

    RULE_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "CURRICULUM400_2", "존재하지 않는 규칙 종류입니다."),

    DUPLICATE_GRADUATION_RULE(HttpStatus.CONFLICT, "CURRICULUM409_2", "동일한 규칙 종류와 규칙 이름의 졸업 규칙이 이미 존재합니다."),

    REQUIREMENT_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM404_3", "존재하지 않는 졸업 요건 세트입니다."),

    DEPARTMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "CURRICULUM400_3", "존재하지 않는 학과입니다."),

    ACTIVE_REQUIREMENT_SET_OVERLAP(
            HttpStatus.CONFLICT, "CURRICULUM409_3", "같은 학과에 적용년도가 겹치는 다른 활성 졸업 요건 세트가 이미 존재합니다. 기존 세트를 먼저 비활성화하세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
