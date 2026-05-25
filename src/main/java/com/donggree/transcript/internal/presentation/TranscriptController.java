package com.donggree.transcript.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.transcript.internal.application.TranscriptService;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.presentation.dto.TranscriptCreateResponse;
import com.donggree.transcript.internal.presentation.swagger.TranscriptApi;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/reports")
@RequiredArgsConstructor
public class TranscriptController implements TranscriptApi {

    private final TranscriptService transcriptService;

    /**
     * PDF를 업로드하여 성적표를 생성한다.
     * 기존 성적표가 있으면 소프트 삭제 후 새로 생성한다.
     *
     * @param memberId 로그인한 회원 ID
     * @param file     취득교과목 영역별 분류표 PDF 파일
     * @return 생성된 성적표의 reportId
     */
    @Override
    @PutMapping
    public ApiResponse<TranscriptCreateResponse> createTranscript(
            @LoginMemberId Long memberId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Long reportId = transcriptService.createTranscript(memberId, file.getBytes());
            return ApiResponse.onSuccess(GeneralSuccessCode.CREATED,
                    new TranscriptCreateResponse(reportId));
        } catch (IOException e) {
            throw new GeneralException(TranscriptErrorCode.INVALID_PDF_FILE);
        }
    }
}
