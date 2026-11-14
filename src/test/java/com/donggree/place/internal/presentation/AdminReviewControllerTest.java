package com.donggree.place.internal.presentation;

import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.AdminReviewCommandService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

class AdminReviewControllerTest extends RestDocsSupport {

    private final AdminReviewCommandService service = Mockito.mock(AdminReviewCommandService.class);

    @Override
    protected Object initController() {
        return new AdminReviewController(service);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 관리자가_리뷰를_숨기고_숨김을_해제한다() throws Exception {
        for (String action : new String[] {"hide", "unhide"}) {
            mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/reviews/{reviewId}/" + action, 30L))
                    .andExpect(status().isOk())
                    .andDo(document(
                            "admin-review-" + action,
                            pathParameters(parameterWithName("reviewId").description("리뷰 ID")),
                            responseFields(
                                    fieldWithPath("isSuccess").description("요청 성공 여부"),
                                    fieldWithPath("code").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("result").optional().description("없음"))));
        }
        then(service).should().hide(30L);
        then(service).should().show(30L);
    }
}
