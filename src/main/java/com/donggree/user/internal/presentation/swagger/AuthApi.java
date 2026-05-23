package com.donggree.user.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.user.internal.presentation.dto.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Operation(
            summary = "로그아웃",
            description = "리프레시 토큰 쿠키를 삭제하여 로그아웃 처리한다. 액세스 토큰은 만료 시까지 유효하다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청"
            )
    })
    ApiResponse<Void> logout(Long memberId, HttpServletRequest request, HttpServletResponse response);
}
