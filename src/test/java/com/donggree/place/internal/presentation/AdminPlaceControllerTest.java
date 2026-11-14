package com.donggree.place.internal.presentation;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.AdminPlaceCommandService;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;

class AdminPlaceControllerTest extends RestDocsSupport {

    private final AdminPlaceCommandService adminPlaceCommandService = Mockito.mock(AdminPlaceCommandService.class);

    @Override
    protected Object initController() {
        return new AdminPlaceController(adminPlaceCommandService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 관리자가_장소를_숨긴다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/places/{placeId}/hide", 1L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-place-hide",
                        pathParameters(parameterWithName("placeId").description("장소 ID")),
                        responseFields(emptyResult())));

        then(adminPlaceCommandService).should().hide(1L);
    }

    @Test
    void 관리자가_장소_숨김을_해제한다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/places/{placeId}/unhide", 1L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-place-unhide",
                        pathParameters(parameterWithName("placeId").description("장소 ID")),
                        responseFields(emptyResult())));

        then(adminPlaceCommandService).should().show(1L);
    }

    @Test
    void 없는_장소는_404다() throws Exception {
        willThrow(new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND))
                .given(adminPlaceCommandService)
                .hide(99L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/places/{placeId}/hide", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE404_1"));
    }

    private static FieldDescriptor[] emptyResult() {
        return new FieldDescriptor[] {
            fieldWithPath("isSuccess").description("요청 성공 여부"),
            fieldWithPath("code").description("응답 코드"),
            fieldWithPath("message").description("응답 메시지"),
            fieldWithPath("result").optional().description("없음")
        };
    }
}
