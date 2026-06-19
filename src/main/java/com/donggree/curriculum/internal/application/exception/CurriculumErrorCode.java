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
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
