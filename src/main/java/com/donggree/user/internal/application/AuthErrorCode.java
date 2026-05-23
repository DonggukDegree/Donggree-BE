package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증 관련 에러 코드.
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_2",
            "유효하지 않은 리프레시 토큰입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
