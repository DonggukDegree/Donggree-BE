package com.donggree.user.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SchoolEmailErrorCode implements BaseErrorCode {
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "EMAIL400_1", "학교 이메일 아이디를 확인해주세요."),
    NOT_REQUESTED(HttpStatus.BAD_REQUEST, "EMAIL400_2", "인증 코드를 먼저 요청해주세요."),
    EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL400_3", "인증 코드가 만료되었습니다. 다시 요청해주세요."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "EMAIL400_4", "인증 코드가 일치하지 않습니다."),
    DELIVERY_PENDING(HttpStatus.CONFLICT, "EMAIL409_3", "인증 메일을 발송 중입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "EMAIL409_1", "이미 인증된 회원입니다."),
    EMAIL_IN_USE(HttpStatus.CONFLICT, "EMAIL409_2", "이미 다른 계정에서 인증한 학교 이메일입니다."),
    RESEND_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "EMAIL429_1", "인증 코드는 60초 후 다시 요청할 수 있습니다."),
    ATTEMPT_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "EMAIL429_2", "인증 시도 횟수를 초과했습니다. 코드를 다시 요청해주세요."),
    DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "EMAIL429_3", "오늘의 인증 메일 발송 횟수를 초과했습니다."),
    QUEUE_FULL(HttpStatus.TOO_MANY_REQUESTS, "EMAIL429_4", "요청이 많습니다. 나중에 시도해주세요."),
    SEND_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL503_1", "발송에 실패하였습니다. 다시 발송해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
