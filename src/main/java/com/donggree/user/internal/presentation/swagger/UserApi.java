package com.donggree.user.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.user.internal.presentation.dto.OnboardingRequest;
import com.donggree.user.internal.presentation.dto.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User", description = "사용자 정보 관리")
public interface UserApi {

    @Operation(
            summary = "온보딩 정보 저장",
            description = "카카오 로그인 후 최초 1회 학번과 이름을 입력받아 회원 정보를 완성한다. 닉네임은 이름과 동일한 값으로 초기 설정된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "온보딩 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 회원"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 온보딩 완료 또는 학번 중복"
            )
    })
    ApiResponse<Void> completeOnboarding(@Parameter(hidden = true) Long memberId, OnboardingRequest request);

    @Operation(
            summary = "사용자 정보 조회",
            description = "로그인한 사용자의 학번, 이름, 닉네임을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 회원"
            )
    })
    ApiResponse<UserInfoResponse> getUserInfo(@Parameter(hidden = true) Long memberId);
}
