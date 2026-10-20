package com.donggree.user.internal.application.exception;

import com.donggree.global.apiPayload.exception.GeneralException;
import lombok.Getter;

/** 원문 SMTP 예외 대신 고정된 실패 분류만 발송 워커에 전달한다. */
@Getter
public class SchoolEmailSendException extends GeneralException {
    public enum Reason {
        CONFIGURATION,
        AUTHENTICATION,
        TIMEOUT,
        CONNECTION,
        SEND_FAILURE,
        UNKNOWN
    }

    private final Reason reason;

    public SchoolEmailSendException(Reason reason) {
        super(SchoolEmailErrorCode.SEND_UNAVAILABLE);
        this.reason = reason;
    }
}
