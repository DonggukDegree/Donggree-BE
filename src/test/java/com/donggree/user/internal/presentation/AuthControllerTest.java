package com.donggree.user.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.support.RestDocsSupport;
import com.donggree.user.internal.application.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthControllerTest extends RestDocsSupport {

    private final AuthService authService = Mockito.mock(AuthService.class);

    @Override
    protected Object initController() {
        return new AuthController(authService);
    }

    @Test
    void 리프레시_토큰으로_새_액세스_토큰을_발급한다() throws Exception {
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        given(authService.refreshAccessToken(refreshToken)).willReturn(newAccessToken);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.accessToken").value(newAccessToken))
                .andDo(document("auth-refresh",
                        requestCookies(
                                cookieWithName("refreshToken").description("리프레시 토큰 (HttpOnly)")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.accessToken").description("새로 발급된 액세스 토큰")
                        )
                ));
    }

    @Test
    void 리프레시_토큰_쿠키가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isBadRequest());
    }
}
