package com.donggree.transcript.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Transcript", description = "성적표 관리")
public interface TranscriptApi {

    @Operation(
            summary = "학업 리포트 생성",
            description = "취득교과목 영역별 분류표 PDF를 업로드하여 성적표를 생성한다. "
                    + "기존 성적표가 있으면 소프트 삭제 후 새로 생성한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "성적표 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 PDF 파일 또는 파싱 실패"
            )
    })
    ApiResponse<TranscriptCreateResponse> createTranscript(
            @Parameter(hidden = true) Long memberId,
            MultipartFile file
    );
}
