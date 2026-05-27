package com.donggree.transcript.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.transcript.internal.application.TranscriptParseResult;
import com.donggree.transcript.internal.application.TranscriptQueryResult;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawCourseRecord;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawMeta;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawSemesterGroup;
import com.donggree.transcript.internal.application.TranscriptService;
import com.donggree.transcript.internal.domain.ParsedCourse;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.presentation.dto.CourseRecordAddRequest;
import com.donggree.transcript.internal.presentation.dto.CourseRecordAddRequest.CourseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TranscriptControllerTest extends RestDocsSupport {

    private final TranscriptService transcriptService = Mockito.mock(TranscriptService.class);
    private final CurriculumLookupService curriculumLookupService = Mockito.mock(CurriculumLookupService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new TranscriptController(transcriptService, curriculumLookupService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long memberId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList())
        );
    }

    @Test
    void 성적표_리포트를_조회한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        RawMeta rawMeta = new RawMeta(1L, 2023, 10L, null, null, null, null, "재학", 60, new BigDecimal("3.50"), 4);
        RawCourseRecord rawRecord = new RawCourseRecord(1L, "CSE1101", "프로그래밍기초", 3, null, "FIRST_MAJOR", "A+", false);
        TranscriptQueryResult queryResult = new TranscriptQueryResult(
                rawMeta, List.of(new RawSemesterGroup("2023-1", List.of(rawRecord))));

        given(transcriptService.getTranscriptRawReport(memberId)).willReturn(queryResult);
        given(curriculumLookupService.findDepartmentNamesByIds(List.of(10L)))
                .willReturn(Map.of(10L, "컴퓨터·AI학부"));

        mockMvc.perform(get("/api/users/me/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andDo(document("transcript-get-report",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.meta.reportId").description("성적표 ID"),
                                fieldWithPath("result.meta.admissionYear").description("입학 연도"),
                                fieldWithPath("result.meta.department").description("전공 학과명"),
                                fieldWithPath("result.meta.subMajor1").type(JsonFieldType.NULL).optional().description("제1부전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.subMajor2").type(JsonFieldType.NULL).optional().description("제2부전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.dualMajor1").type(JsonFieldType.NULL).optional().description("제1복수전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.dualMajor2").type(JsonFieldType.NULL).optional().description("제2복수전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.academicStatus").description("학적 상태 (재학/휴학/졸업 등)"),
                                fieldWithPath("result.meta.totalCredits").description("총 취득 학점"),
                                fieldWithPath("result.meta.gpa").description("평점 평균"),
                                fieldWithPath("result.meta.completedSemesters").description("이수 학기 수"),
                                fieldWithPath("result.courses[].semester").description("학기 (예: 2023-1, 2023-여름)"),
                                fieldWithPath("result.courses[].records[].id").description("수강 이력 ID"),
                                fieldWithPath("result.courses[].records[].courseCode").description("과목 코드"),
                                fieldWithPath("result.courses[].records[].courseName").description("과목명"),
                                fieldWithPath("result.courses[].records[].credits").description("학점"),
                                fieldWithPath("result.courses[].records[].courseType").description("이수 구분 (COMMON_GENERAL, LIBERAL_ARTS, ACADEMIC_FOUNDATION, FIRST_MAJOR, SECOND_MAJOR, FREE_ELECTIVE)"),
                                fieldWithPath("result.courses[].records[].areaName").type(JsonFieldType.NULL).optional().description("이수 영역명 (없으면 null)"),
                                fieldWithPath("result.courses[].records[].grade").description("성적 (A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP)"),
                                fieldWithPath("result.courses[].records[].retake").description("재수강 여부")
                        )
                ));
    }

    @Test
    void PDF를_업로드하여_성적표를_생성한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        Map<String, String> meta = Map.of(
                "학과", "컴퓨터·AI학부",
                "교육과정 적용년도", "2023",
                "학적상태", "재학",
                "과정", "학사",
                "총취득학점", "60",
                "평점평균", "3.50",
                "이수학기", "4"
        );
        ParsedTranscriptData parsedData = new ParsedTranscriptData(meta, List.of(
                new ParsedCourse("2023-1", 1, "전공", "CSE1101", "프로그래밍기초", 3, "A+", null, false)
        ));
        TranscriptParseResult parseResult = new TranscriptParseResult(parsedData, "{}");

        TranscriptCreateData createData = new TranscriptCreateData(
                memberId, "{}", 2023, "재학", "학사", 10L,
                null, null, null, null, 60, new BigDecimal("3.50"), 4,
                null, false, false, false, false, false, null, null, false
        );

        given(transcriptService.parseTranscript(any(byte[].class))).willReturn(parseResult);
        given(curriculumLookupService.findDepartmentIdByName("컴퓨터·AI학부")).willReturn(Optional.of(10L));
        given(curriculumLookupService.findDepartmentIdByName(isNull())).willReturn(Optional.empty());
        given(transcriptService.buildCreateData(any(), any(), any(), any(), any(), any(), any(), any())).willReturn(createData);
        given(transcriptService.createTranscript(any(), any(), any(), any())).willReturn(1L);

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "transcript.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/reports")
                        .file(pdfFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.reportId").value(1))
                .andDo(document("transcript-create",
                        requestParts(
                                partWithName("file").description("성적표 PDF 파일 (nDRIMS '취득교과목 영역별 분류표')")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.reportId").description("생성된 성적표 ID")
                        )
                ));
    }

    @Test
    void 수강_이력을_수동으로_추가한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        CourseRecordAddRequest request = new CourseRecordAddRequest(List.of(
                new CourseItem("2024-1", "FIRST_MAJOR", "전공필수", "CSE2101", "자료구조", 3, "B+", false)
        ));

        given(transcriptService.addCourseRecords(any(), any())).willReturn(List.of(10L));

        mockMvc.perform(patch("/api/users/me/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.addedIds[0]").value(10))
                .andDo(document("transcript-add-course-records",
                        requestFields(
                                fieldWithPath("courses").description("추가할 수강 이력 목록"),
                                fieldWithPath("courses[].semester").description("학기 (예: 2024-1, 2024-여름)"),
                                fieldWithPath("courses[].courseType").description("이수 구분 (COMMON_GENERAL, LIBERAL_ARTS, ACADEMIC_FOUNDATION, FIRST_MAJOR, SECOND_MAJOR, FREE_ELECTIVE, 대소문자 무관)"),
                                fieldWithPath("courses[].areaName").optional().description("이수 영역명 (없으면 null 또는 생략)"),
                                fieldWithPath("courses[].courseCode").description("과목 코드"),
                                fieldWithPath("courses[].courseName").description("과목명"),
                                fieldWithPath("courses[].credits").description("학점"),
                                fieldWithPath("courses[].grade").description("성적 (A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP)"),
                                fieldWithPath("courses[].retake").description("재수강 여부")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.addedIds").type(JsonFieldType.ARRAY).description("새로 추가된 수강 이력 ID 목록")
                        )
                ));
    }
}
