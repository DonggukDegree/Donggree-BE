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
    INVALID_REPORT(HttpStatus.BAD_REQUEST, "PLACE400_4", "신고 내용을 확인해주세요. 기타 사유는 설명이 필요합니다."),
    INVALID_REVIEW(HttpStatus.BAD_REQUEST, "PLACE400_5", "리뷰를 확인해주세요. 별점은 1~5점, 본문은 10~500자입니다."),
    SELF_REVIEW_REPORT(HttpStatus.BAD_REQUEST, "PLACE400_6", "본인 리뷰는 신고할 수 없습니다."),
    VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "PLACE403_1", "학생 인증 후 이용할 수 있습니다."),
    NOT_REVIEW_AUTHOR(HttpStatus.FORBIDDEN, "PLACE403_2", "본인 리뷰만 수정·삭제할 수 있습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_1", "존재하지 않는 장소입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_2", "존재하지 않는 리뷰입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_3", "존재하지 않는 신고입니다."),
    EDIT_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_4", "존재하지 않는 수정 이력입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_5", "존재하지 않거나 탈퇴한 회원입니다."),
    DUPLICATE_PLACE(HttpStatus.CONFLICT, "PLACE409_1", "이미 등록된 장소입니다."),
    EDIT_CONFLICT(HttpStatus.CONFLICT, "PLACE409_2", "다른 회원이 먼저 수정했습니다. 최신 정보를 확인해주세요."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "PLACE409_3", "이미 신고한 장소입니다. 처리된 뒤 다시 신고할 수 있습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "PLACE409_4", "이미 리뷰를 작성한 장소입니다."),
    DUPLICATE_REVIEW_REPORT(HttpStatus.CONFLICT, "PLACE409_5", "이미 신고한 리뷰입니다. 처리된 뒤 다시 신고할 수 있습니다."),
    REVIEW_HIDDEN(HttpStatus.CONFLICT, "PLACE409_6", "숨겨진 리뷰는 수정할 수 없습니다."),
    REPORT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "PLACE409_7", "이미 처리된 신고입니다."),
    SUSPENSION_TARGET_MISSING(HttpStatus.CONFLICT, "PLACE409_8", "탈퇴했거나 연결이 끊긴 회원은 정지할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
