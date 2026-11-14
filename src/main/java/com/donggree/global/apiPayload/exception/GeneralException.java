package com.donggree.global.apiPayload.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
@org.springframework.modulith.NamedInterface("api")
public class GeneralException extends RuntimeException {

    private final BaseErrorCode code;

    private final String stage;

    /** 실패 응답 result에 함께 담을 값. 대부분 null이며, 정지 회원 안내처럼 화면에 추가 정보가 필요할 때만 쓴다. */
    private final Object result;

    public GeneralException(BaseErrorCode code) {
        this(code, null, "application");
    }

    public GeneralException(BaseErrorCode code, Object result) {
        super(code.getMessage());
        this.code = code;
        this.stage = "application";
        this.result = result;
    }

    public GeneralException(BaseErrorCode code, Throwable cause, String stage) {
        super(code.getMessage(), cause);
        this.code = code;
        this.stage = stage;
        this.result = null;
    }
}
