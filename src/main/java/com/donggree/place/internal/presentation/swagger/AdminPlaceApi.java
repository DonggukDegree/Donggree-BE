package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Place", description = "관리자 동글맵 장소 숨김·숨김 해제. ADMIN 이상")
public interface AdminPlaceApi {

    @Operation(summary = "장소 숨김", description = "장소를 지도·상세에서 숨기고(404 처리) 그 장소의 처리 대기 신고를 처리 완료로 바꾼다. 이미 숨긴 장소도 성공.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "숨김 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 장소")
    })
    ApiResponse<Void> hidePlace(@Parameter(description = "장소 ID") Long placeId);

    @Operation(summary = "장소 숨김 해제", description = "숨긴 장소를 다시 지도에 보이게 하고 그 장소의 처리 대기 신고를 기각한다. 이미 보이는 장소도 성공.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "숨김 해제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 장소")
    })
    ApiResponse<Void> unhidePlace(@Parameter(description = "장소 ID") Long placeId);

    @Operation(
            summary = "장소 수정 되돌리기",
            description = "수정 이력 한 건의 수정 전 값으로 영업시간·설명·메뉴·태그를 되돌린다. 되돌린 것도 관리자 수정으로 이력에 남는다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "되돌리기 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없는 장소·수정 이력")
    })
    ApiResponse<Void> revertEdit(
            Long adminId,
            @Parameter(description = "장소 ID") Long placeId,
            @Parameter(description = "수정 이력 ID") Long historyId);
}
