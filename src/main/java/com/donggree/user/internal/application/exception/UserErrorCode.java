package com.donggree.user.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 사용자 관련 에러 코드.
 */
@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER404_1",
            "존재하지 않는 회원입니다."),

    ALREADY_ONBOARDED(HttpStatus.CONFLICT,
            "USER409_1",
            "이미 온보딩이 완료된 회원입니다."),

    DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT,
            "USER409_2",
            "이미 사용 중인 학번입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
