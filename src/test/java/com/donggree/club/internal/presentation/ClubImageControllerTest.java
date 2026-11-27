package com.donggree.club.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.club.internal.application.ClubImageService;
import com.donggree.club.internal.application.readmodel.ClubImageFile;
import com.donggree.club.internal.application.readmodel.ClubImageReadModel;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ClubImageControllerTest extends RestDocsSupport {

    private static final String NAME = "3f2b6c1e-5d4a-4b7e-9a1c-2e8f0d6b7c9a.webp";

    private final ClubImageService imageService = Mockito.mock(ClubImageService.class);

    @Override
    protected Object initController() {
        return new ClubImageController(imageService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 사진을_올린다() throws Exception {
        given(imageService.uploadByMember(eq(1L), any()))
                .willReturn(new ClubImageReadModel(21L, "/api/clubs/images/" + NAME));

        mockMvc.perform(RestDocumentationRequestBuilders.multipart("/api/clubs/images")
                        .file(new MockMultipartFile("file", "band.webp", "image/webp", new byte[] {1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(21))
                .andDo(document(
                        "club-image-upload",
                        requestParts(partWithName("file").description("JPG·PNG·WEBP 사진(5MB 이하, 파일 내용으로 형식 확인)")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.id").description("사진 ID. 등록·수정 요청의 imageIds에 넣는다"),
                                fieldWithPath("result.url").description("사진 주소(API 서버 기준 경로)"))));
    }

    @Test
    void 사진을_오래_캐시하도록_내려준다() throws Exception {
        given(imageService.load(NAME)).willReturn(new ClubImageFile("image/webp", new byte[] {1, 2, 3}));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs/images/{storedName}", NAME))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/webp"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")))
                .andDo(document(
                        "club-image-download",
                        pathParameters(parameterWithName("storedName").description("사진 주소의 마지막 부분"))));
    }
}
