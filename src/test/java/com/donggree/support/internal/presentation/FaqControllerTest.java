package com.donggree.support.internal.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.support.RestDocsSupport;
import com.donggree.support.internal.application.FaqQueryService;
import com.donggree.support.internal.application.projection.FaqProjection;
import com.donggree.support.internal.domain.enums.FaqTag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FaqControllerTest extends RestDocsSupport {

    private final FaqQueryService faqQueryService = Mockito.mock(FaqQueryService.class);

    @Override
    protected Object initController() {
        return new FaqController(faqQueryService);
    }

    @Test
    void FAQ_목록을_전체_조회한다() throws Exception {
        given(faqQueryService.getFaqs(null))
                .willReturn(List.of(
                        new FaqProjection(2L, FaqTag.MAJOR, "복수전공은 지원하나요?", "지금은 주전공 기준으로만 판정하고 있어요."),
                        new FaqProjection(1L, FaqTag.COMMON, "성적표는 어디서 받나요?", "nDRIMS에서 PDF로 내려받을 수 있어요.")));

        mockMvc.perform(get("/api/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(2))
                .andExpect(jsonPath("$.result[0].tag").value("MAJOR"))
                .andExpect(jsonPath("$.result[1].title").value("성적표는 어디서 받나요?"))
                .andDo(document(
                        "faq-list",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("FAQ ID"),
                                fieldWithPath("result[].tag").description("태그(SERVICE=서비스, COMMON=공통, MAJOR=전공)"),
                                fieldWithPath("result[].title").description("제목(화면의 Q. 문구)"),
                                fieldWithPath("result[].content").description("본문(줄바꿈 포함 평문)"))));
    }

    @Test
    void FAQ_목록을_태그로_걸러_조회한다() throws Exception {
        given(faqQueryService.getFaqs(FaqTag.MAJOR))
                .willReturn(List.of(new FaqProjection(2L, FaqTag.MAJOR, "복수전공은 지원하나요?", "지금은 주전공 기준으로만 판정하고 있어요.")));

        mockMvc.perform(get("/api/faqs").param("tag", "MAJOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].tag").value("MAJOR"))
                .andDo(document(
                        "faq-list-by-tag",
                        queryParameters(parameterWithName("tag")
                                .optional()
                                .description("태그 필터(SERVICE=서비스, COMMON=공통, MAJOR=전공). 미지정이면 전체")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("FAQ ID"),
                                fieldWithPath("result[].tag").description("태그(SERVICE=서비스, COMMON=공통, MAJOR=전공)"),
                                fieldWithPath("result[].title").description("제목(화면의 Q. 문구)"),
                                fieldWithPath("result[].content").description("본문(줄바꿈 포함 평문)"))));
    }
}
