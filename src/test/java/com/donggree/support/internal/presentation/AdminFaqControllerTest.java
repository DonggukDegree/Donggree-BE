package com.donggree.support.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.support.internal.application.FaqCommandService;
import com.donggree.support.internal.application.command.FaqCommand;
import com.donggree.support.internal.domain.enums.FaqTag;
import com.donggree.support.internal.presentation.dto.FaqRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

class AdminFaqControllerTest extends RestDocsSupport {

    private final FaqCommandService faqCommandService = Mockito.mock(FaqCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new AdminFaqController(faqCommandService);
    }

    // 태그 누락 시의 검증 실패(VALID400_1) 응답 봉투를 확인하기 위해 전역 예외 핸들러를 등록한다.
    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void FAQ를_등록한다() throws Exception {
        FaqRequest request = new FaqRequest(FaqTag.COMMON, "성적표는 어디서 받나요?", "nDRIMS에서 PDF로 내려받을 수 있어요.");
        given(faqCommandService.create(any(FaqCommand.class))).willReturn(100L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(100))
                .andDo(document(
                        "admin-faq-create",
                        requestFields(
                                fieldWithPath("tag").description("태그(SERVICE=서비스, COMMON=공통, MAJOR=전공). 필수"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("본문(줄바꿈 포함 평문)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("생성된 FAQ ID"))));
    }

    @Test
    void 태그를_생략하면_400을_반환한다() throws Exception {
        // 태그는 기본값으로 대신 채우지 않는다. 분류를 빠뜨린 글이 한 태그에 조용히 쌓이는 것을 막기 위해 거부한다.
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("VALID400_1"));

        then(faqCommandService).should(never()).create(any(FaqCommand.class));
    }

    @Test
    void FAQ를_수정한다() throws Exception {
        FaqRequest request = new FaqRequest(FaqTag.MAJOR, "복수전공은 지원하나요?", "지금은 주전공 기준으로만 판정하고 있어요.");

        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/admin/faqs/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty())
                .andDo(document(
                        "admin-faq-update",
                        pathParameters(parameterWithName("id").description("FAQ ID")),
                        requestFields(
                                fieldWithPath("tag").description("태그(SERVICE=서비스, COMMON=공통, MAJOR=전공). 필수"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("본문(줄바꿈 포함 평문)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("없음"))));
    }

    @Test
    void FAQ를_삭제한다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/admin/faqs/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty())
                .andDo(document(
                        "admin-faq-delete",
                        pathParameters(parameterWithName("id").description("FAQ ID")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("없음"))));

        then(faqCommandService).should().delete(1L);
    }
}
