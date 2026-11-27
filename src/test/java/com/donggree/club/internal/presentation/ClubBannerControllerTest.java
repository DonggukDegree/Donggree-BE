package com.donggree.club.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.club.internal.application.ClubBannerQueryService;
import com.donggree.club.internal.application.admin.AdminClubBannerCommandService;
import com.donggree.club.internal.application.command.BannerSaveCommand;
import com.donggree.club.internal.application.readmodel.ClubBannerReadModel;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;

class ClubBannerControllerTest extends RestDocsSupport {

    private static final ClubBannerReadModel TEXT_BANNER = new ClubBannerReadModel(
            1L,
            "우리 동아리가 없나요?\n등록하면 500P",
            "EVENT",
            "처음 등록한 동아리가 승인되면 포인트를 드려요.",
            "linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)",
            null,
            null,
            "/clubs/register",
            null,
            LocalDateTime.of(2026, 12, 31, 23, 59),
            true,
            1);

    private final ClubBannerQueryService queryService = Mockito.mock(ClubBannerQueryService.class);
    private final AdminClubBannerCommandService commandService = Mockito.mock(AdminClubBannerCommandService.class);

    @Override
    protected Object initController() {
        return new AdminClubBannerController(queryService, commandService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 관리자_배너_목록은_노출_조건과_순서를_준다() throws Exception {
        given(queryService.getAllBanners()).willReturn(List.of(TEXT_BANNER));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/admin/clubs/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].order").value(1))
                .andDo(document(
                        "admin-club-banner-list",
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result[]").description("전체 배너(노출 순서)"))
                                .andWithPrefix("result[].", adminFields())));
    }

    @Test
    void 배너를_등록하고_시간대를_한국_시간으로_바꾼다() throws Exception {
        ArgumentCaptor<BannerSaveCommand> captor = ArgumentCaptor.forClass(BannerSaveCommand.class);
        given(commandService.create(captor.capture())).willReturn(3L);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/admin/clubs/banners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title":"우리 동아리가 없나요?","eyebrow":"EVENT","description":null,
                                "background":"#2563eb","imageUrl":null,"mobileImageUrl":null,"link":"/clubs/register",
                                "startsAt":"2026-11-30T15:00:00Z","endsAt":null,"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(3))
                .andDo(document(
                        "admin-club-banner-create",
                        requestFields(saveFields()),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("등록한 배너 ID(맨 뒤 순서)"))));

        org.assertj.core.api.Assertions.assertThat(captor.getValue().startsAt())
                .isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0));
    }

    @Test
    void 배너를_수정_삭제하고_순서를_옮긴다() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.put("/api/admin/clubs/banners/{bannerId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title":"광고 이미지","eyebrow":null,"description":null,"background":null,
                                "imageUrl":"/api/clubs/images/banner.webp","mobileImageUrl":null,
                                "link":"https://pf.kakao.com/_x/chat","startsAt":null,"endsAt":null,"active":false}
                                """))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-banner-revise",
                        pathParameters(parameterWithName("bannerId").description("배너 ID")),
                        requestFields(saveFields())));
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/admin/clubs/banners/{bannerId}/move", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offset\":-1}"))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-banner-move",
                        pathParameters(parameterWithName("bannerId").description("배너 ID")),
                        requestFields(fieldWithPath("offset").description("한 칸 앞(-1)·뒤(+1). 맨 앞·뒤면 그대로"))));
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/admin/clubs/banners/{bannerId}", 1L))
                .andExpect(status().isOk())
                .andDo(document(
                        "admin-club-banner-delete",
                        pathParameters(parameterWithName("bannerId").description("배너 ID"))));

        verify(commandService).revise(eq(1L), any());
        verify(commandService).move(1L, -1);
        verify(commandService).delete(1L);
    }

    static List<FieldDescriptor> publicFields() {
        return List.of(
                fieldWithPath("id").description("배너 ID"),
                fieldWithPath("title").description("제목(줄바꿈 포함 30자). 이미지 배너는 대체 텍스트"),
                fieldWithPath("eyebrow").optional().description("글자 배너의 작은 라벨(12자)"),
                fieldWithPath("description").optional().description("글자 배너의 설명(60자)"),
                fieldWithPath("imageUrl").optional().description("PC 이미지. 있으면 이미지 배너"),
                fieldWithPath("mobileImageUrl").optional().description("모바일 이미지. 없으면 PC 이미지"),
                fieldWithPath("background").optional().description("글자 배너의 CSS 배경"),
                fieldWithPath("link").description("동그리 경로(같은 창) 또는 https 주소(새 창)"));
    }

    private static List<FieldDescriptor> adminFields() {
        List<FieldDescriptor> fields = new ArrayList<>(publicFields());
        fields.addAll(List.of(
                fieldWithPath("active").description("노출 켜짐"),
                fieldWithPath("startsAt").optional().description("노출 시작(한국 시간, 포함). null이면 바로"),
                fieldWithPath("endsAt").optional().description("노출 종료(한국 시간, 제외). null이면 계속"),
                fieldWithPath("order").description("노출 순서(작을수록 먼저)")));
        return fields;
    }

    private static List<FieldDescriptor> saveFields() {
        return List.of(
                fieldWithPath("title").description("제목(1~30자). 이미지 배너는 대체 텍스트"),
                fieldWithPath("eyebrow").optional().description("글자 배너 라벨(12자 이내)"),
                fieldWithPath("description").optional().description("글자 배너 설명(60자 이내)"),
                fieldWithPath("background").optional().description("글자 배너 필수 CSS 배경(영문·숫자·# ( ) , . % - 공백)"),
                fieldWithPath("imageUrl").optional().description("이미지 배너 PC 이미지(동그리 경로 또는 https)"),
                fieldWithPath("mobileImageUrl").optional().description("이미지 배너 모바일 이미지"),
                fieldWithPath("link").description("동그리 경로('/'로 시작, '//' 제외) 또는 https 주소"),
                fieldWithPath("startsAt").optional().description("노출 시작(시간대 포함 ISO 8601)"),
                fieldWithPath("endsAt").optional().description("노출 종료(시간대 포함 ISO 8601, 시작 이후)"),
                fieldWithPath("active").description("노출 켜짐"));
    }
}
