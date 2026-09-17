package com.donggree.support.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.support.internal.presentation.dto.FaqRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin-FAQ", description = "관리자의 자주 묻는 질문 등록·수정·삭제")
public interface AdminFaqApi {

    @Operation(
            summary = "FAQ 등록",
            description = "FAQ를 등록하고 생성된 ID를 반환한다. tag는 SERVICE(서비스)·COMMON(공통)·MAJOR(전공) 중 하나이며, "
                    + "반드시 하나를 지정해야 한다. 누락 시 400.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공")})
    ApiResponse<Long> createFaq(FaqRequest request);

    @Operation(summary = "FAQ 수정", description = "FAQ의 태그·제목·본문을 전체 교체한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 FAQ")
    })
    ApiResponse<Void> updateFaq(@Parameter(description = "FAQ ID") Long id, FaqRequest request);

    @Operation(summary = "FAQ 삭제", description = "FAQ를 삭제한다. 되돌릴 수 없다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 FAQ")
    })
    ApiResponse<Void> deleteFaq(@Parameter(description = "FAQ ID") Long id);
}
