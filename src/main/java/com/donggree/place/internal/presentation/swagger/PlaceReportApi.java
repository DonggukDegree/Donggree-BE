package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.place.internal.presentation.dto.PlaceReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Place Report", description = "동글맵 장소 신고. 등록자 포함 인증 학생")
public interface PlaceReportApi {

    @Operation(
            summary = "장소 신고",
            description = "PDF 본인인증 또는 학교 이메일 인증 회원이 장소를 신고하고 신고 ID를 받는다. "
                    + "서로 다른 회원의 처리 대기 신고가 3건이 되면 장소를 자동 숨김하고, 그 밖의 처리는 관리자가 한다. "
                    + "같은 장소에 처리 대기 신고가 있으면 409, 처리된 뒤에는 다시 신고할 수 있다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 접수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류, 기타 사유의 설명 누락"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "학생 인증 미완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 숨김 장소"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "처리 대기 신고 존재")
    })
    ApiResponse<Long> reportPlace(
            Long memberId, @Parameter(description = "장소 ID") Long placeId, PlaceReportRequest request);
}
