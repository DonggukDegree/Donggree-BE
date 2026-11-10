package com.donggree.place.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 동글맵 장소 관련 에러 코드. */
@Getter
@AllArgsConstructor
public enum PlaceErrorCode implements BaseErrorCode {
    OUT_OF_SERVICE_AREA(HttpStatus.BAD_REQUEST, "PLACE400_1", "동글맵 지역(캠퍼스 반경 1.5km) 안에서만 장소를 등록할 수 있습니다."),
    CATEGORY_MISMATCH(HttpStatus.BAD_REQUEST, "PLACE400_2", "상세카테고리가 카테고리와 맞지 않습니다."),
    INVALID_PLACE(HttpStatus.BAD_REQUEST, "PLACE400_3", "장소 정보를 확인해주세요."),
    VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "PLACE403_1", "학생 인증 후 이용할 수 있습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_1", "존재하지 않는 장소입니다."),
    DUPLICATE_PLACE(HttpStatus.CONFLICT, "PLACE409_1", "이미 등록된 장소입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
