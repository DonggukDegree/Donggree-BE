package com.donggree.global.handler;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

    // 애플리케이션에서 발생하는 커스텀 예외를 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(GeneralException ex) {

        return ResponseEntity.status(ex.getCode().getStatus()).body(ApiResponse.onFailure(ex.getCode()));
    }

    // 컨트롤러 메서드에서 @Valid 어노테이션을 사용하여 DTO의 유효성 검사를 수행
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        // 검사에 실패한 필드와 그에 대한 메시지를 저장하는 Map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        GeneralErrorCode code = GeneralErrorCode.VALID_FAIL;
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(code, errors);

        // 에러 코드, 메시지와 함께 errors를 반환
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    // 잘못된 요청으로 인한 클라이언트 오류는 400으로 응답한다.
    // - MethodArgumentTypeMismatchException: 쿼리 파라미터 타입 변환 실패 (예: courseType에 enum에 없는 값)
    // - MissingServletRequestParameterException: 필수 쿼리 파라미터 누락
    // - MissingRequestCookieException: 필수 쿠키 누락 (예: /auth/refresh 호출 시 refreshToken 쿠키 없음)
    // - HttpMessageNotReadableException: 요청 본문 누락 또는 JSON 형식 오류
    // 이전에는 이들이 미처리 예외로 빠져 500으로 응답되던 문제를 바로잡는다.
    // 디버깅을 위해 사유는 warn 레벨로만 기록한다(스택 트레이스 미기록).
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        MissingRequestCookieException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("[BadRequest] {}", ex.getMessage());

        GeneralErrorCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code));
    }

    // 지원하지 않는 HTTP 메서드로 요청한 경우 405로 응답한다. (이전에는 500으로 빠지던 문제 수정)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("[MethodNotAllowed] {}", ex.getMessage());

        GeneralErrorCode code = GeneralErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code));
    }

    // 그 외의 정의되지 않은 모든 예외 처리 (스택 트레이스를 로그에 기록)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("[UnhandledException] {}", ex.getMessage(), ex);

        GeneralErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code));
    }
}
