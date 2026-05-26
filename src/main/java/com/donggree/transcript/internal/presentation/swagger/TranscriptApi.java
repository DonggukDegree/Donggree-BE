package com.donggree.transcript.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptCreateResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse;
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

    @Operation(
            summary = "학업 정보 조회",
            description = "로그인한 회원의 성적표 메타 정보와 수강 이력을 커서 기반으로 조회한다. "
                    + "수강 이력은 학기별로 그룹핑하여 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "성적표 없음"
            )
    })
    ApiResponse<TranscriptReportResponse> getTranscriptReport(
            @Parameter(hidden = true) Long memberId,
            @Parameter(description = "마지막으로 조회한 course_record ID (첫 조회 시 생략)") Long cursor,
            @Parameter(description = "한 번에 가져올 수강 이력 수 (기본값 20)") int size
    );
}
