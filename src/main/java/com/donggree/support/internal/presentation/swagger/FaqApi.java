package com.donggree.support.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.support.internal.domain.enums.FaqTag;
import com.donggree.support.internal.presentation.dto.FaqResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "FAQ", description = "자주 묻는 질문 조회(읽기 전용, 비로그인 허용)")
public interface FaqApi {

    @Operation(
            summary = "FAQ 목록 조회",
            description = "FAQ를 최신순으로 조회한다. tag를 주면 해당 태그만, 미지정이면 전체(화면 상단 '전체' 칩)를 반환한다. "
                    + "목록 한 건이 곧 아코디언 한 줄이라 본문까지 함께 내려준다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<FaqResponse>> getFaqs(
            @Parameter(description = "태그 필터(SERVICE=서비스, COMMON=공통, MAJOR=전공). 미지정=전체") FaqTag tag);
}
