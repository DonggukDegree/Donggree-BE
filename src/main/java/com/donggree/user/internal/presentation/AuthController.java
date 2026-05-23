package com.donggree.user.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.user.internal.application.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(
            @CookieValue("refreshToken") String refreshToken
    ) {
        String accessToken = authService.refreshAccessToken(refreshToken);
        TokenRefreshResponse response = new TokenRefreshResponse(accessToken);

        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
