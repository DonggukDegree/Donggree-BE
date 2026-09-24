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

    DUPLICATE_GRADUATION_RULE(HttpStatus.CONFLICT, "CURRICULUM409_2", "규칙 종류, 규칙 이름, 규칙 옵션이 모두 동일한 졸업 규칙이 이미 존재합니다."),

    INVALID_GRADUATION_RULE_CONFIG(HttpStatus.BAD_REQUEST, "CURRICULUM400_5", "규칙 옵션은 올바른 JSON 객체여야 합니다."),

    DUPLICATE_GRADUATION_RULE_ID(HttpStatus.BAD_REQUEST, "CURRICULUM400_6", "한 번의 요청에서 같은 졸업 규칙을 두 번 수정할 수 없습니다."),

    REQUIREMENT_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "CURRICULUM404_3", "존재하지 않는 졸업 요건 세트입니다."),

    DEPARTMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "CURRICULUM400_3", "존재하지 않는 학과입니다."),

    INVALID_MAJOR_ROLE_CONFIG(
            HttpStatus.BAD_REQUEST,
            "CURRICULUM400_4",
            "MIN_CREDITS·REQUIRED_COURSE의 규칙 적용 대상을 허용된 값으로 하나 이상 선택해야 합니다."),

    ACTIVE_REQUIREMENT_SET_OVERLAP(
            HttpStatus.CONFLICT, "CURRICULUM409_3", "같은 학과에 적용년도가 겹치는 다른 활성 졸업 요건 세트가 이미 존재합니다. 기존 세트를 먼저 비활성화하세요."),

    DEPARTMENT_COLLEGE_MISMATCH(HttpStatus.CONFLICT, "CURRICULUM409_4", "이미 다른 단과대에 속한 학과입니다. 학과의 소속 단과대는 변경할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
