package com.donggree.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@org.springframework.modulith.NamedInterface("api")
public enum GeneralErrorCode implements BaseErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400_1", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401_1", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403_1", "요청이 거부되었습니다."),
    // 신고 처리·관리자 판단으로 이용이 제한된 회원. 응답 result에 정지 종료 시각·일수·사유 구분을 담는다.
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "AUTH403_2", "이용이 제한된 회원입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404_1", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405_1", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500_1", "예기치 않은 서버 에러가 발생했습니다."),
    VALID_FAIL(HttpStatus.BAD_REQUEST, "VALID400_1", "검증에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
