package com.donggree.user.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.support.RestDocsSupport;
import com.donggree.user.internal.application.UserService;
import com.donggree.user.internal.application.dto.UserInfoResponse;
import com.donggree.user.internal.presentation.dto.OnboardingRequest;
import com.donggree.user.internal.presentation.dto.UserInfoUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UserControllerTest extends RestDocsSupport {

    private final UserService userService = Mockito.mock(UserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new UserController(userService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 온보딩_정보를_저장한다() throws Exception {
        Long memberId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList())
        );

        OnboardingRequest request = new OnboardingRequest("2023123456", "하승연");

        mockMvc.perform(post("/api/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result").isEmpty())
                .andDo(document("user-onboarding",
                        requestFields(
                                fieldWithPath("studentId").description("학번"),
                                fieldWithPath("name").description("이름")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("없음")
                        )
                ));

        Mockito.verify(userService).completeOnboarding(memberId, "2023123456", "하승연");
    }

    @Test
    void 사용자_정보를_조회한다() throws Exception {
        Long memberId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList())
        );

        given(userService.getUserInfo(memberId))
                .willReturn(new UserInfoResponse("2023123456", "하승연", "하승연", false));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.studentId").value("2023123456"))
                .andExpect(jsonPath("$.result.name").value("하승연"))
                .andExpect(jsonPath("$.result.nickname").value("하승연"))
                .andExpect(jsonPath("$.result.identityVerified").value(false))
                .andDo(document("user-info",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.studentId").description("학번"),
                                fieldWithPath("result.name").description("이름"),
                                fieldWithPath("result.nickname").description("닉네임"),
                                fieldWithPath("result.identityVerified").description("본인 인증 완료 여부")
                        )
                ));
    }

    @Test
    void 사용자_정보를_수정한다() throws Exception {
        Long memberId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList())
        );

        UserInfoUpdateRequest request = new UserInfoUpdateRequest("2023123456", "하승연", "동동이");

        given(userService.updateUserInfo(memberId, "2023123456", "하승연", "동동이"))
                .willReturn(new UserInfoResponse("2023123456", "하승연", "동동이", false));

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.studentId").value("2023123456"))
                .andExpect(jsonPath("$.result.name").value("하승연"))
                .andExpect(jsonPath("$.result.nickname").value("동동이"))
                .andExpect(jsonPath("$.result.identityVerified").value(false))
                .andDo(document("user-info-update",
                        requestFields(
                                fieldWithPath("studentId").description("학번"),
                                fieldWithPath("name").description("이름"),
                                fieldWithPath("nickname").description("닉네임")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.studentId").description("학번"),
                                fieldWithPath("result.name").description("이름"),
                                fieldWithPath("result.nickname").description("닉네임"),
                                fieldWithPath("result.identityVerified").description("본인 인증 완료 여부")
                        )
                ));
    }
}
