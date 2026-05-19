package com.donggree.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
        "COMMON200_1",
        "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED,
        "COMMON201_1",
        "리소스가 성공적으로 생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT,
        "COMMON204_1",
        "성공했지만 반환할 데이터는 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
