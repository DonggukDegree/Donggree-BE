package com.donggree.transcript.internal.application.exception;

import com.donggree.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Transcript 모듈 에러 코드.
 */
@Getter
@AllArgsConstructor
public enum TranscriptErrorCode implements BaseErrorCode {

    PDF_PARSING_FAILED(HttpStatus.BAD_REQUEST,
            "TRANSCRIPT400_1",
            "PDF 파싱에 실패했습니다."),

    INVALID_PDF_FILE(HttpStatus.BAD_REQUEST,
            "TRANSCRIPT400_2",
            "유효하지 않은 PDF 파일입니다."),

    TRANSCRIPT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "TRANSCRIPT404_1",
            "성적표를 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
