package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.place.internal.presentation.dto.PlaceDetailResponse;
import com.donggree.place.internal.presentation.dto.PlaceDuplicatesResponse;
import com.donggree.place.internal.presentation.dto.PlaceRegisterRequest;
import com.donggree.place.internal.presentation.dto.PlaceSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Place", description = "동글맵 장소 조회·등록. 조회는 로그인 회원, 등록은 인증 학생")
public interface PlaceApi {

    @Operation(summary = "지도 장소 목록 조회", description = "숨기지 않은 장소 전체를 지도 핀 표시용 최소 정보로 반환한다. 카테고리 필터는 FE에서 적용한다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<PlaceSummaryResponse>> getPlaces();

    @Operation(summary = "장소 상세 조회", description = "장소 정보와 메뉴, 최초 등록일·마지막 수정일을 반환한다. 숨김 장소는 404.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 숨김 장소")
    })
    ApiResponse<PlaceDetailResponse> getPlace(@Parameter(description = "장소 ID") Long placeId);

    @Operation(
            summary = "등록 전 중복 장소 확인",
            description = "핀 반경 50m 안에서 정규화 이름이 같은 장소(exact, 등록 불가)와 한쪽이 다른 쪽을 포함하는 장소(similar, 안내)를 반환한다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공")})
    ApiResponse<PlaceDuplicatesResponse> findDuplicates(
            @Parameter(description = "등록할 장소 이름") String name,
            @Parameter(description = "핀 위도") double latitude,
            @Parameter(description = "핀 경도") double longitude);

    @Operation(summary = "장소 등록", description = "PDF 본인인증 또는 학교 이메일 인증 회원만 등록한다. 생성된 장소 ID를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "입력값 오류, 서비스 영역 밖, 카테고리 짝 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "학생 인증 미완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "반경 50m 안 같은 이름 장소 존재")
    })
    ApiResponse<Long> registerPlace(Long memberId, PlaceRegisterRequest request);
}
