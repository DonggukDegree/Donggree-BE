package com.donggree.transcript.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.transcript.internal.presentation.dto.CourseRecordAddRequest;
import com.donggree.transcript.internal.presentation.dto.CourseRecordAddResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Transcript", description = "성적표 관리")
public interface TranscriptApi {

    @Operation(
            summary = "학업 리포트 생성",
            description = "취득교과목 영역별 분류표 PDF를 업로드하여 성적표를 생성한다. " + "기존 성적표가 있으면 소프트 삭제 후 새로 생성한다.")
    @RequestBody(
            content =
                    @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schemaProperties =
                                    @SchemaProperty(
                                            name = "file",
                                            schema =
                                                    @Schema(
                                                            type = "string",
                                                            format = "binary",
                                                            description = "성적표 PDF 파일"))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "성적표 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 PDF 파일 또는 파싱 실패")
    })
    ApiResponse<Void> createTranscript(@Parameter(hidden = true) Long memberId, MultipartFile file);

    @Operation(
            summary = "사용자 학업 정보 조회",
            description = "로그인한 회원의 성적표 메타 정보와 수강 이력 전체를 조회한다. " + "수강 이력은 학기 오름차순으로 그룹핑하여 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성적표 없음")
    })
    ApiResponse<TranscriptReportResponse> getTranscriptReport(@Parameter(hidden = true) Long memberId);

    @Operation(
            summary = "사용자 학업 정보 수정",
            description = "PDF 파싱 결과에 없는 수강 이력을 수동으로 추가한다. " + "여러 개를 한 번에 추가할 수 있다. 성적표가 없으면 404를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수강 이력 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 이수구분 또는 성적 값"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성적표 없음")
    })
    ApiResponse<CourseRecordAddResponse> addCourseRecords(
            @Parameter(hidden = true) Long memberId, CourseRecordAddRequest request);
}
