package com.donggree.user.internal.application.exception;

import com.donggree.global.apiPayload.exception.GeneralException;

/** 잘못된 코드 응답에서도 실패 횟수는 커밋해야 한다. */
public class SchoolEmailCodeMismatchException extends GeneralException {
    public SchoolEmailCodeMismatchException(boolean locked) {
        super(locked ? SchoolEmailErrorCode.ATTEMPT_LIMIT : SchoolEmailErrorCode.INVALID_CODE);
    }
}
