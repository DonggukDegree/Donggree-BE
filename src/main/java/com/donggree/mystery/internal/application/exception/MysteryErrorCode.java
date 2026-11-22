package com.donggree.mystery.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MysteryErrorCode implements BaseErrorCode {
    INVALID_OPTION(HttpStatus.BAD_REQUEST, "MYSTERY400_1", "오늘의 후보에서 학과, 장소, 수단을 골라주세요."),
    DATE_CHANGED(HttpStatus.CONFLICT, "MYSTERY409_1", "새로운 사건이 시작됐어요. 오늘의 사건을 다시 불러올게요."),
    FINISHED(HttpStatus.CONFLICT, "MYSTERY409_2", "오늘의 사건은 이미 종료됐어요."),
    DUPLICATE(HttpStatus.CONFLICT, "MYSTERY409_3", "이미 제출한 조합이에요. 다른 조합으로 추리해주세요."),
    REQUEST_REUSED(HttpStatus.CONFLICT, "MYSTERY409_4", "같은 요청 번호로 다른 추리를 제출할 수 없어요.");
    private final HttpStatus status;
    private final String code;
    private final String message;
}
