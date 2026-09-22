package com.donggree.transcript.internal.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import com.donggree.transcript.internal.application.TranscriptCommandService;
import com.donggree.transcript.internal.application.TranscriptQueryService;
import com.donggree.transcript.internal.application.command.TranscriptCreateResult;
import com.donggree.transcript.internal.application.command.TranscriptParseResult;
import com.donggree.transcript.internal.application.command.TranscriptUpdateResult;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawCourseRecord;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawMeta;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawSemesterGroup;
import com.donggree.transcript.internal.domain.ParsedCourse;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.presentation.dto.CourseRecordUpdateRequest;
import com.donggree.transcript.internal.presentation.dto.CourseRecordUpdateRequest.CourseItem;
import com.donggree.user.MemberIdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private final TranscriptQueryService transcriptQueryService = Mockito.mock(TranscriptQueryService.class);
    private final TranscriptCommandService transcriptCommandService = Mockito.mock(TranscriptCommandService.class);
    private final CurriculumLookupService curriculumLookupService = Mockito.mock(CurriculumLookupService.class);
    private final MemberIdentityService memberIdentityService = Mockito.mock(MemberIdentityService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new TranscriptController(
                transcriptQueryService, transcriptCommandService, curriculumLookupService, memberIdentityService);
    }

    // 검증 실패(VALID400_1) 등 예외 응답 봉투를 확인하기 위해 전역 예외 핸들러를 등록한다.
    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long memberId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList()));
    }

    @Test
    void 성적표_리포트를_조회한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        RawMeta rawMeta = new RawMeta(
                2023,
                10L,
                null,
                null,
                null,
                null,
                "재학",
                60,
                new BigDecimal("3.50"),
                new BigDecimal("4.50"),
                null,
                4,
                LocalDateTime.of(2024, 3, 1, 9, 0, 0),
                LocalDateTime.of(2024, 6, 1, 9, 0, 0));
        RawCourseRecord rawRecord = new RawCourseRecord(1L, "CSE1101", "프로그래밍기초", 3, null, "전공", "A+", false);
        TranscriptReportProjection queryResult =
                new TranscriptReportProjection(rawMeta, List.of(new RawSemesterGroup("2023-1", List.of(rawRecord))));

        given(transcriptQueryService.getTranscriptRawReport(memberId)).willReturn(queryResult);
        given(curriculumLookupService.findDepartmentNamesByIds(List.of(10L))).willReturn(Map.of(10L, "컴퓨터·AI학부"));
        given(curriculumLookupService.findCollegeNameByDepartmentId(10L)).willReturn(Optional.of("정보통신공학대학"));

        mockMvc.perform(get("/api/users/me/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.meta.collegeName").value("정보통신공학대학"))
                .andExpect(jsonPath("$.result.meta.majorGpa").value(4.5))
                .andExpect(jsonPath("$.result.meta.dualMajor1Gpa").value(org.hamcrest.Matchers.nullValue()))
                .andDo(document(
                        "transcript-get-report",
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.meta.admissionYear").description("입학 연도"),
                                fieldWithPath("result.meta.collegeName")
                                        .type(JsonFieldType.STRING)
                                        .optional()
                                        .description("소속 단과대학명 (학과 미등록 시 null)"),
                                fieldWithPath("result.meta.department").description("전공 학과명"),
                                fieldWithPath("result.meta.subMajor1")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("제1부전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.subMajor2")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("제2부전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.dualMajor1")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("제1복수전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.dualMajor2")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("제2복수전공 학과명 (없으면 null)"),
                                fieldWithPath("result.meta.academicStatus").description("학적 상태 (재학/휴학/졸업 등)"),
                                fieldWithPath("result.meta.totalCredits").description("총 취득 학점"),
                                fieldWithPath("result.meta.gpa").description("평점 평균"),
                                fieldWithPath("result.meta.majorGpa")
                                        .type(JsonFieldType.NUMBER)
                                        .optional()
                                        .description("현재 전공·전필 과목의 학점 가중평점 (F 포함, P·NP 제외, 계산 가능한 학점이 없으면 null)"),
                                fieldWithPath("result.meta.dualMajor1Gpa")
                                        .type(JsonFieldType.NUMBER)
                                        .optional()
                                        .description("현재 복수1 과목의 학점 가중평점 (복수전공 미등록 또는 계산 가능한 학점이 없으면 null)"),
                                fieldWithPath("result.meta.completedSemesters").description("이수 학기 수"),
                                fieldWithPath("result.meta.createdAt").description("성적표 최초 생성 시각"),
                                fieldWithPath("result.meta.updatedAt").description("성적표 최종 수정 시각"),
                                fieldWithPath("result.courses[].semester").description("학기 (예: 2023-1, 2023-여름)"),
                                fieldWithPath("result.courses[].records[].id").description("수강 이력 ID"),
                                fieldWithPath("result.courses[].records[].courseCode")
                                        .description("과목 코드"),
                                fieldWithPath("result.courses[].records[].courseName")
                                        .description("과목명"),
                                fieldWithPath("result.courses[].records[].credits")
                                        .description("학점"),
                                fieldWithPath("result.courses[].records[].courseType")
                                        .description("이수 구분 — PDF 원시 문자열 (예: 전공, 복수1, 복수2, 공교, 일교, 학기)"),
                                fieldWithPath("result.courses[].records[].areaName")
                                        .type(JsonFieldType.NULL)
                                        .optional()
                                        .description("이수 영역명 (없으면 null)"),
                                fieldWithPath("result.courses[].records[].grade")
                                        .description("성적 (A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP)"),
                                fieldWithPath("result.courses[].records[].retake")
                                        .description("재수강 여부"))));
    }

    @Test
    void 복수전공_평점과_계산불가_전공평점을_구별하여_반환한다() throws Exception {
        authenticate(1L);
        RawMeta rawMeta = new RawMeta(
                2023,
                10L,
                null,
                null,
                20L,
                null,
                "재학",
                3,
                new BigDecimal("3.50"),
                null,
                new BigDecimal("0.00"),
                4,
                LocalDateTime.of(2024, 3, 1, 9, 0),
                LocalDateTime.of(2024, 6, 1, 9, 0));
        given(transcriptQueryService.getTranscriptRawReport(1L))
                .willReturn(new TranscriptReportProjection(rawMeta, List.of()));
        given(curriculumLookupService.findDepartmentNamesByIds(List.of(10L, 20L)))
                .willReturn(Map.of(10L, "주전공학과", 20L, "복수전공학과"));

        mockMvc.perform(get("/api/users/me/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.meta.gpa").value(3.5))
                .andExpect(jsonPath("$.result.meta.dualMajor1").value("복수전공학과"))
                .andExpect(jsonPath("$.result.meta.majorGpa").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.meta.dualMajor1Gpa").value(0.0));
    }

    @Test
    void PDF를_업로드하여_성적표를_생성한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        Map<String, String> meta = Map.of(
                "학과", "컴퓨터·AI학부",
                "복수1", "전자전기공학부",
                "교육과정 적용년도", "2023",
                "학적상태", "재학",
                "과정", "학사",
                "총취득학점", "60",
                "평점평균", "3.50",
                "이수학기", "4",
                "학번", "2023123456",
                "성명", "홍길동");
        ParsedTranscriptData parsedData = new ParsedTranscriptData(
                meta, List.of(new ParsedCourse("2023-1", 1, "전공", "CSE1101", "프로그래밍기초", 3, "A+", null, false)));
        TranscriptParseResult parseResult = new TranscriptParseResult(parsedData, "{}");

        TranscriptCreateData createData = new TranscriptCreateData(
                memberId,
                "{}",
                2023,
                "재학",
                "학사",
                10L,
                null,
                null,
                20L,
                null,
                60,
                new BigDecimal("3.50"),
                4,
                null,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                false);

        given(transcriptCommandService.parseTranscript(any(byte[].class))).willReturn(parseResult);
        given(curriculumLookupService.findDepartmentIdByName("컴퓨터·AI학부")).willReturn(Optional.of(10L));
        given(curriculumLookupService.findDepartmentIdByName("전자전기공학부")).willReturn(Optional.of(20L));
        given(curriculumLookupService.findDepartmentIdByName(isNull())).willReturn(Optional.empty());
        given(transcriptCommandService.buildCreateData(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(createData);
        given(transcriptCommandService.createTranscript(any(), any(), any(), any()))
                .willReturn(new TranscriptCreateResult(60, 54, 6));

        MockMultipartFile pdfFile =
                new MockMultipartFile("file", "transcript.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/reports").file(pdfFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.totalCredits").value(60))
                .andExpect(jsonPath("$.result.recordedCredits").value(54))
                .andExpect(jsonPath("$.result.creditGap").value(6))
                .andDo(document(
                        "transcript-create",
                        requestParts(partWithName("file").description("성적표 PDF 파일 (nDRIMS '취득교과목 영역별 분류표')")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.totalCredits").description("PDF에 기재된 총취득학점"),
                                fieldWithPath("result.recordedCredits").description("등록된 수강 이력 학점의 합"),
                                fieldWithPath("result.creditGap")
                                        .description("총취득학점 - 과목 학점 합. 0이 아니면 불일치(양수: 이수 이력 추가 필요, 음수: 과목 합이 더 많음)"))));

        then(transcriptCommandService).should().buildCreateData(memberId, "{}", parsedData, 10L, null, null, 20L, null);
    }

    @Test
    void 수강_이력_전체를_수정하고_재계산된_학점과_평점을_반환한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        // 3학점 A+ 3과목 + 3학점 B+ 2과목 = 총 15학점, GPA 4.10 (재계산 결과를 서비스가 반환한다고 가정)
        CourseRecordUpdateRequest request = new CourseRecordUpdateRequest(List.of(
                new CourseItem("2024-1", "전공", "전공필수", "CSE2101", "자료구조", 3, "A+", false),
                new CourseItem("2024-1", "전공", "전공필수", "CSE2102", "알고리즘", 3, "B+", false)));

        TranscriptUpdateResult result = new TranscriptUpdateResult(
                15,
                new BigDecimal("4.10"),
                List.of(new RawSemesterGroup(
                        "2024-1",
                        List.of(
                                new RawCourseRecord(10L, "CSE2101", "자료구조", 3, "전공필수", "전공", "A+", false),
                                new RawCourseRecord(11L, "CSE2102", "알고리즘", 3, "전공필수", "전공", "B+", false)))));
        given(transcriptCommandService.replaceCourseRecords(any(), any())).willReturn(result);

        mockMvc.perform(patch("/api/users/me/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.totalCredits").value(15))
                .andExpect(jsonPath("$.result.gpa").value(4.10))
                .andExpect(jsonPath("$.result.courses[0].records[0].id").value(10))
                .andDo(document(
                        "transcript-update-course-records",
                        requestFields(
                                fieldWithPath("courses").description("변경 후의 전체 수강 이력 목록 (이 목록으로 기존 이력을 통째 치환)"),
                                fieldWithPath("courses[].semester").description("학기 (예: 2024-1, 2024-여름)"),
                                fieldWithPath("courses[].courseType")
                                        .description("이수 구분 — PDF 원시 문자열 그대로 입력 (예: 전공, 공교, 일교, 학기)"),
                                fieldWithPath("courses[].areaName").optional().description("이수 영역명 (없으면 null 또는 생략)"),
                                fieldWithPath("courses[].courseCode").description("과목 코드"),
                                fieldWithPath("courses[].courseName").description("과목명"),
                                fieldWithPath("courses[].credits").description("학점"),
                                fieldWithPath("courses[].grade")
                                        .description("성적 (A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP)"),
                                fieldWithPath("courses[].retake").description("재수강 여부")),
                        responseFields(
                                fieldWithPath("isSuccess").description("요청 성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("result.totalCredits").description("재계산된 총취득학점 (F·NP 제외, 이수 성공 학점의 합)"),
                                fieldWithPath("result.gpa")
                                        .description("재계산된 평점 평균 (Σ(등급 평점 × 학점) ÷ 평점 계산용 학점. P·NP는 계산 제외, F는 분모 포함)"),
                                fieldWithPath("result.courses[].semester").description("학기 (학기 오름차순 그룹)"),
                                fieldWithPath("result.courses[].records[].id").description("수강 이력 ID (치환 후 새로 부여됨)"),
                                fieldWithPath("result.courses[].records[].courseCode")
                                        .description("과목 코드"),
                                fieldWithPath("result.courses[].records[].courseName")
                                        .description("과목명"),
                                fieldWithPath("result.courses[].records[].credits")
                                        .description("학점"),
                                fieldWithPath("result.courses[].records[].courseType")
                                        .description("이수 구분 — PDF 원시 문자열"),
                                fieldWithPath("result.courses[].records[].areaName")
                                        .optional()
                                        .description("이수 영역명 (없으면 null)"),
                                fieldWithPath("result.courses[].records[].grade")
                                        .description("성적 (A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP)"),
                                fieldWithPath("result.courses[].records[].retake")
                                        .description("재수강 여부"))));
    }

    @Test
    void 학점이_0인_수강_이력도_수정에_포함할_수_있다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        CourseRecordUpdateRequest request = new CourseRecordUpdateRequest(
                List.of(new CourseItem("2024-1", "전공", null, "GEN0000", "영점학점과목", 0, "P", false)));

        TranscriptUpdateResult result = new TranscriptUpdateResult(
                0,
                new BigDecimal("0.00"),
                List.of(new RawSemesterGroup(
                        "2024-1", List.of(new RawCourseRecord(10L, "GEN0000", "영점학점과목", 0, null, "전공", "P", false)))));
        given(transcriptCommandService.replaceCourseRecords(any(), any())).willReturn(result);

        mockMvc.perform(patch("/api/users/me/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    void 학점이_음수이면_400을_반환한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        CourseRecordUpdateRequest request = new CourseRecordUpdateRequest(
                List.of(new CourseItem("2024-1", "전공", "전공필수", "CSE2101", "자료구조", -1, "B+", false)));

        mockMvc.perform(patch("/api/users/me/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("VALID400_1"));
    }

    @Test
    void 학점_필드가_누락되면_400을_반환한다() throws Exception {
        Long memberId = 1L;
        authenticate(memberId);

        // credits 필드를 아예 누락한 요청 (Integer + @NotNull로 0 기본값 통과를 방지)
        CourseRecordUpdateRequest request = new CourseRecordUpdateRequest(
                List.of(new CourseItem("2024-1", "전공", "전공필수", "CSE2101", "자료구조", null, "B+", false)));

        mockMvc.perform(patch("/api/users/me/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("VALID400_1"));
    }
}
