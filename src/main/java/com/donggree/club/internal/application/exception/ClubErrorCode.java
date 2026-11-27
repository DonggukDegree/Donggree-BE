package com.donggree.club.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ClubErrorCode implements BaseErrorCode {
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "CLUB400_1", "목록 위치가 올바르지 않아요. 처음부터 다시 불러와주세요."),
    INVALID_CLUB(HttpStatus.BAD_REQUEST, "CLUB400_2", "동아리 정보를 확인해주세요."),
    INVALID_RECRUITMENT(HttpStatus.BAD_REQUEST, "CLUB400_3", "모집 정보를 확인해주세요."),
    INVALID_REQUESTER(HttpStatus.BAD_REQUEST, "CLUB400_4", "동아리와의 관계와 확인 연락처(이메일 또는 전화번호)를 입력해주세요."),
    REQUEST_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "CLUB400_5", "요청 종류에 맞는 내용으로 다시 요청해주세요."),
    INVALID_REJECT_REASON(HttpStatus.BAD_REQUEST, "CLUB400_6", "반려 사유를 1~200자로 입력해주세요."),
    INVALID_BANNER(HttpStatus.BAD_REQUEST, "CLUB400_7", "배너 내용을 확인해주세요."),
    VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "CLUB403_1", "학생 인증 후 이용할 수 있습니다."),
    CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "CLUB404_1", "존재하지 않는 동아리입니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CLUB404_2", "존재하지 않는 요청입니다."),
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "CLUB404_3", "존재하지 않는 배너입니다."),
    DUPLICATE_PENDING(HttpStatus.CONFLICT, "CLUB409_1", "이미 검토 중인 요청이 있어요. 내 요청 내역에서 수정해서 다시 요청해주세요."),
    REQUEST_CLOSED(HttpStatus.CONFLICT, "CLUB409_2", "검토 중이거나 반려된 요청만 다시 요청할 수 있어요."),
    EDIT_CONFLICT(HttpStatus.CONFLICT, "CLUB409_3", "다른 회원이 먼저 수정했어요. 새로고침 후 다시 시도해주세요."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "CLUB409_4", "이미 처리된 요청이에요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
