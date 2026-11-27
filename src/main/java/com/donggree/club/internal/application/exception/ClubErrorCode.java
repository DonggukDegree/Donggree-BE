package com.donggree.club.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ClubErrorCode implements BaseErrorCode {
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "CLUB400_1", "목록 위치가 올바르지 않아요. 처음부터 다시 불러와주세요."),
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "CLUB404_1", "존재하지 않는 동아리입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
