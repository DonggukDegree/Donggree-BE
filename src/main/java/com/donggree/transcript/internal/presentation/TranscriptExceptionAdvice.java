package com.donggree.transcript.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = TranscriptController.class)
public class TranscriptExceptionAdvice {

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPdfFile(MissingServletRequestPartException ex) {
        return ResponseEntity.status(TranscriptErrorCode.PDF_FILE_REQUIRED.getStatus())
                .body(ApiResponse.onFailure(TranscriptErrorCode.PDF_FILE_REQUIRED));
    }
}
