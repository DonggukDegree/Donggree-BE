package com.donggree.user.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.user.internal.application.SchoolEmailVerificationCommandService;
import com.donggree.user.internal.application.UserQueryService;
import com.donggree.user.internal.application.command.RequestSchoolEmailCodeCommand;
import com.donggree.user.internal.application.command.SchoolEmailCodeRequestResult;
import com.donggree.user.internal.application.command.VerifySchoolEmailCodeCommand;
import com.donggree.user.internal.application.readmodel.SchoolEmailVerificationReadModel;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SchoolEmailVerificationControllerTest extends RestDocsSupport {
    private final UserQueryService queries = Mockito.mock(UserQueryService.class);
    private final SchoolEmailVerificationCommandService service =
            Mockito.mock(SchoolEmailVerificationCommandService.class);
    private static final String ENDPOINT = "/api/users/me/school-email-verification";

    @Override
    protected Object initController() {
        return new SchoolEmailVerificationController(queries, service);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 학교_이메일_인증_상태를_조회한다() throws Exception {
        given(queries.getSchoolEmailVerification(1L))
                .willReturn(new SchoolEmailVerificationReadModel(false, true, "student@dgu.ac.kr"));
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.schoolEmailVerified").value(true))
                .andExpect(jsonPath("$.result.identityVerified").value(false))
                .andDo(document(
                        "school-email-status",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.identityVerified").description("기존 PDF 본인 인증 여부"),
                                fieldWithPath("result.schoolEmailVerified").description("학교 이메일 인증 여부"),
                                fieldWithPath("result.schoolEmail").description("인증된 학교 이메일, 미인증이면 null"),
                                fieldWithPath("result.deliveryStatus")
                                        .description("메일 상태: IDLE, QUEUED, SENDING, SENT, FAILED"),
                                fieldWithPath("result.requestId").description("최신 발송 요청 식별자, 미요청이면 null"),
                                fieldWithPath("result.requestedEmail").description("발송 대상 이메일, 미요청이면 null"),
                                fieldWithPath("result.expiresInSeconds").description("실제 발송 후 코드 유효 시간(초), 발송 전에는 0"),
                                fieldWithPath("result.resendAfterSeconds").description("재발송까지 남은 시간(초), 발송 실패 시 0"))));
    }

    @Test
    void 고정된_학교_도메인으로_코드를_요청하고_응답에는_코드를_포함하지_않는다() throws Exception {
        given(service.requestCode(1L, new RequestSchoolEmailCodeCommand("student")))
                .willReturn(new SchoolEmailCodeRequestResult("student@dgu.ac.kr", 0, 60));
        mockMvc.perform(post(ENDPOINT + "/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localPart\":\"student\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("student@dgu.ac.kr"))
                .andExpect(jsonPath("$.result.code").doesNotExist())
                .andDo(document(
                        "school-email-request-code",
                        requestFields(fieldWithPath("localPart").description("도메인을 제외한 학교 이메일 아이디, @dgu.ac.kr 고정")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.email").description("발송 대상 이메일"),
                                fieldWithPath("result.expiresInSeconds").description("코드의 남은 유효 시간(초)"),
                                fieldWithPath("result.resendAfterSeconds").description("재발송 대기시간(초), 요청 접수부터 적용"),
                                fieldWithPath("result.requestId").description("발송 요청 식별자"),
                                fieldWithPath("result.deliveryStatus").description("접수된 메일의 상태 QUEUED"))));
    }

    @Test
    void 여섯자리_코드와_입력한_아이디를_확인한다() throws Exception {
        mockMvc.perform(post(ENDPOINT + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localPart\":\"student\",\"code\":\"012345\"}"))
                .andExpect(status().isOk())
                .andDo(document(
                        "school-email-confirm",
                        requestFields(
                                fieldWithPath("localPart").description("발송 요청 시 입력한 아이디"),
                                fieldWithPath("code").description("숫자 6자리 인증 코드, 앞자리 0 유지")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("없음"))));
        verify(service).verifyCode(1L, new VerifySchoolEmailCodeCommand("student", "012345"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "1234567", "12345a"})
    void 여섯자리_숫자가_아닌_코드는_검증기에_전달하지_않는다(String code) throws Exception {
        mockMvc.perform(post(ENDPOINT + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localPart\":\"student\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void 빈_아이디를_거부한다() throws Exception {
        mockMvc.perform(post(ENDPOINT + "/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localPart\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
