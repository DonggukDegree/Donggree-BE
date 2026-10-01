package com.donggree.global.apiPayload.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseErrorCode code;

    private final String stage;

    public GeneralException(BaseErrorCode code) {
        this(code, null, "application");
    }

    public GeneralException(BaseErrorCode code, Throwable cause, String stage) {
        super(code.getMessage(), cause);
        this.code = code;
        this.stage = stage;
    }
}
