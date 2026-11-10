package com.donggree.place.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.place.internal.application.PlaceCommandService;
import com.donggree.place.internal.application.PlaceQueryService;
import com.donggree.place.internal.application.command.RegisterPlaceCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.application.readmodel.PlaceDuplicatesReadModel;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.presentation.dto.PlaceRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PlaceControllerTest extends RestDocsSupport {

    private static final Long MEMBER_ID = 1L;

    private final PlaceQueryService placeQueryService = Mockito.mock(PlaceQueryService.class);
    private final PlaceCommandService placeCommandService = Mockito.mock(PlaceCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new PlaceController(placeQueryService, placeCommandService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(MEMBER_ID, null, Collections.emptyList()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 지도_장소_목록을_조회한다() throws Exception {
        given(placeQueryService.getPlaces())
                .willReturn(List.of(new PlaceSummaryReadModel(
                        1L, "동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, 37.5585, 126.999)));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(1))
                .andExpect(jsonPath("$.result[0].category").value("RESTAURANT"))
                .andDo(document(
                        "place-list",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result[].id").description("장소 ID"),
                                fieldWithPath("result[].name").description("장소 이름"),
                                fieldWithPath("result[].category").description("카테고리 코드"),
                                fieldWithPath("result[].subCategory").description("상세카테고리 코드"),
                                fieldWithPath("result[].latitude").description("위도"),
                                fieldWithPath("result[].longitude").description("경도"))));
    }

    @Test
    void 장소_상세를_조회한다() throws Exception {
        given(placeQueryService.getPlace(1L))
                .willReturn(new PlaceDetailReadModel(
                        1L,
                        "동국반점",
                        PlaceCategory.RESTAURANT,
                        PlaceSubCategory.CHINESE,
                        37.5585,
                        126.999,
                        "평일 11:00-21:00",
                        "정문 맞은편 골목 2층",
                        List.of(new PlaceDetailReadModel.Menu("짜장면", 7000), new PlaceDetailReadModel.Menu("탕수육", null)),
                        LocalDateTime.of(2026, 10, 26, 12, 0),
                        LocalDateTime.of(2026, 10, 27, 9, 30)));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/places/{placeId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.menus[1].price").isEmpty())
                .andDo(document(
                        "place-detail",
                        pathParameters(parameterWithName("placeId").description("장소 ID")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.id").description("장소 ID"),
                                fieldWithPath("result.name").description("장소 이름"),
                                fieldWithPath("result.category").description("카테고리 코드"),
                                fieldWithPath("result.subCategory").description("상세카테고리 코드"),
                                fieldWithPath("result.latitude").description("위도. 주소는 FE가 이 좌표로 실시간 조회"),
                                fieldWithPath("result.longitude").description("경도"),
                                fieldWithPath("result.businessHours")
                                        .optional()
                                        .description("영업시간(자유 텍스트). 미입력 시 null"),
                                fieldWithPath("result.description").optional().description("설명. 미입력 시 null"),
                                fieldWithPath("result.menus[].name").description("메뉴명(입력 순서)"),
                                fieldWithPath("result.menus[].price").optional().description("가격(원). null이면 가격 변동"),
                                fieldWithPath("result.createdAt").description("최초 등록 시각"),
                                fieldWithPath("result.updatedAt").description("마지막 정보 수정 시각"))));
    }

    @Test
    void 없는_장소는_404다() throws Exception {
        given(placeQueryService.getPlace(99L)).willThrow(new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/places/{placeId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE404_1"));
    }

    @Test
    void 등록_전_중복_후보를_확인한다() throws Exception {
        var exact = new PlaceDuplicatesReadModel.Candidate(
                1L, "동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, 12);
        var similar = new PlaceDuplicatesReadModel.Candidate(
                2L, "동국반점 2호점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, 35);
        given(placeQueryService.findDuplicates("동국 반점", 37.5585, 126.999))
                .willReturn(new PlaceDuplicatesReadModel(List.of(exact), List.of(similar)));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/places/duplicates")
                        .param("name", "동국 반점")
                        .param("latitude", "37.5585")
                        .param("longitude", "126.999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.exact[0].id").value(1))
                .andExpect(jsonPath("$.result.similar[0].distanceMeters").value(35))
                .andDo(document(
                        "place-duplicates",
                        queryParameters(
                                parameterWithName("name").description("등록할 장소 이름"),
                                parameterWithName("latitude").description("핀 위도"),
                                parameterWithName("longitude").description("핀 경도")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result.exact[]")
                                                .description("반경 50m 안 정규화 이름이 같은 장소. 있으면 등록 불가"),
                                        fieldWithPath("result.similar[]")
                                                .description("반경 50m 안 한쪽 이름이 다른 쪽을 포함하는 장소. 안내용"))
                                .andWithPrefix("result.exact[].", candidateFields())
                                .andWithPrefix("result.similar[].", candidateFields())));
    }

    @Test
    void 장소를_등록한다() throws Exception {
        PlaceRegisterRequest request = new PlaceRegisterRequest(
                "동국반점",
                PlaceCategory.RESTAURANT,
                PlaceSubCategory.CHINESE,
                37.5585,
                126.999,
                "평일 11:00-21:00",
                "정문 맞은편 골목 2층",
                List.of(new PlaceRegisterRequest.Menu("짜장면", 7000), new PlaceRegisterRequest.Menu("탕수육", null)));
        given(placeCommandService.register(eq(MEMBER_ID), any(RegisterPlaceCommand.class)))
                .willReturn(10L);

        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(10))
                .andDo(document(
                        "place-register",
                        requestFields(
                                fieldWithPath("name").description("장소 이름(1~30자). 등록 후 변경 불가"),
                                fieldWithPath("category").description("카테고리 코드. 등록 후 변경 불가"),
                                fieldWithPath("subCategory").description("상세카테고리 코드. 카테고리에 속해야 함"),
                                fieldWithPath("latitude").description("지도 중심 핀 위도. 캠퍼스 반경 1.5km 이내"),
                                fieldWithPath("longitude").description("지도 중심 핀 경도"),
                                fieldWithPath("businessHours").optional().description("영업시간(최대 100자, 선택)"),
                                fieldWithPath("description").optional().description("설명(최대 300자, 선택)"),
                                fieldWithPath("menus[]").optional().description("메뉴 목록(최대 50개, 입력 순서 저장)"),
                                fieldWithPath("menus[].name").description("메뉴명(1~30자)"),
                                fieldWithPath("menus[].price").optional().description("가격(0~1,000,000원). 비우면 가격 변동")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result").description("생성된 장소 ID"))));
    }

    @Test
    void 미인증_회원의_등록은_403이다() throws Exception {
        given(placeCommandService.register(eq(MEMBER_ID), any(RegisterPlaceCommand.class)))
                .willThrow(new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED));

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"동국반점\",\"category\":\"RESTAURANT\",\"subCategory\":\"CHINESE\",\"latitude\":37.5585,\"longitude\":126.999}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLACE403_1"));
    }

    @Test
    void 필수값이_없거나_메뉴_가격이_범위를_벗어나면_400이다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"name\":\" \",\"category\":\"RESTAURANT\",\"latitude\":37.5585,\"longitude\":126.999,"
                                        + "\"menus\":[{\"name\":\"짜장면\",\"price\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALID400_1"));

        then(placeCommandService).should(never()).register(any(), any());
    }

    private static FieldDescriptor[] candidateFields() {
        return new FieldDescriptor[] {
            fieldWithPath("id").description("장소 ID"),
            fieldWithPath("name").description("장소 이름"),
            fieldWithPath("category").description("카테고리 코드"),
            fieldWithPath("subCategory").description("상세카테고리 코드"),
            fieldWithPath("distanceMeters").description("핀과의 거리(m, 반올림)")
        };
    }
}
