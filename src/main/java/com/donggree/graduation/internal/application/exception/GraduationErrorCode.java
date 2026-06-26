package com.donggree.graduation.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GraduationErrorCode implements BaseErrorCode {
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "GRADUATION404_1", "학업 리포트를 찾을 수 없습니다."),
    REQUIREMENT_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "GRADUATION404_2", "적용 가능한 졸업 요건을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
