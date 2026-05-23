package com.donggree.user.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CookieValue;

@Tag(name = "Auth", description = "OAuth2 로그인 및 토큰 관리")
public interface AuthApi {

    @Operation(
            summary = "액세스 토큰 갱신",
            description = "HttpOnly 쿠키의 리프레시 토큰으로 새 액세스 토큰을 발급한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "액세스 토큰 갱신 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰"
            )
    })
    ApiResponse<TokenRefreshResponse> refresh(
            @CookieValue("refreshToken") String refreshToken
    );
}
