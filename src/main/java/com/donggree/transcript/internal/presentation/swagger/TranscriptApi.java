package com.donggree.transcript.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.transcript.internal.presentation.dto.CourseRecordUpdateRequest;
import com.donggree.transcript.internal.presentation.dto.CourseRecordUpdateResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptCreateResponse;
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
            description = "취득교과목 영역별 분류표 PDF를 업로드하여 성적표를 생성한다. " + "기존 성적표가 있으면 소프트 삭제 후 새로 생성한다. "
                    + "응답의 creditGap(총취득학점 - 파싱된 과목 학점 합)이 0이 아니면 PDF와 과목 이력이 불일치하므로 "
                    + "프론트엔드는 사용자에게 수강 이력 추가를 안내할 수 있다.")
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
    ApiResponse<TranscriptCreateResponse> createTranscript(@Parameter(hidden = true) Long memberId, MultipartFile file);

    @Operation(
            summary = "사용자 학업 정보 조회",
            description = "로그인한 회원의 성적표 메타 정보와 수강 이력 전체를 조회한다. "
                    + "메타에는 단과대학명·학과·총취득학점·평점 평균·성적표 생성/수정 시각이 포함된다. "
                    + "전공·복수1 평점은 현재 수강 이력으로 계산하며 계산 가능한 학점이 없으면 null이다. "
                    + "수강 이력은 학기 오름차순으로 그룹핑하여 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성적표 없음")
    })
    ApiResponse<TranscriptReportResponse> getTranscriptReport(@Parameter(hidden = true) Long memberId);

    @Operation(
            summary = "사용자 학업 정보 수정",
            description = "변경 후의 전체 수강 이력 목록을 받아 기존 이력을 통째 치환한다. "
                    + "목록에 없는 기존 이력은 삭제, 새 항목은 추가되어 수정·삭제·추가를 한 번에 반영한다. "
                    + "치환 후 총취득학점과 평점 평균(GPA)을 재계산하여 전체 수강 이력과 함께 반환한다. "
                    + "메타 정보는 수정할 수 없다. 성적표가 없으면 404를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수강 이력 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 이수구분 또는 성적 값"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성적표 없음")
    })
    ApiResponse<CourseRecordUpdateResponse> updateCourseRecords(
            @Parameter(hidden = true) Long memberId, CourseRecordUpdateRequest request);
}
