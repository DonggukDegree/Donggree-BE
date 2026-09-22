package com.donggree.graduation.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.graduation.internal.application.GraduationQueryService;
import com.donggree.graduation.internal.presentation.dto.AdminReportPreviewResponse;
import com.donggree.transcript.TranscriptPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 관리자 전용 미리보기. 파싱과 판정 모듈의 공개 인터페이스를 조합하며 저장 경로는 호출하지 않는다. */
@RestController
@RequestMapping("/api/admin/reports/preview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Report Preview", description = "관리자 일회성 PDF 리포트")
public class AdminReportPreviewController {
    private final TranscriptPreviewService transcriptPreviewService;
    private final GraduationQueryService graduationQueryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "PDF 리포트 미리보기", description = "본인확인·DB 저장 없이 사용자와 같은 졸업 판정 요약과 전체 영역 상세 반환")
    public ResponseEntity<ApiResponse<AdminReportPreviewResponse>> preview(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            var transcript = transcriptPreviewService.preview(file == null ? null : file.getBytes());
            var result = graduationQueryService.preview(transcript);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, AdminReportPreviewResponse.from(result)));
        } catch (IOException e) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
    }
}
