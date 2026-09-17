package com.donggree.support.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 고객지원(FAQ) 관련 에러 코드.
 */
@Getter
@AllArgsConstructor
public enum SupportErrorCode implements BaseErrorCode {
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPORT404_1", "존재하지 않는 FAQ입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
