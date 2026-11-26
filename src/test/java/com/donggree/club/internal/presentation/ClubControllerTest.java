package com.donggree.club.internal.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.donggree.club.internal.application.ClubQueryService;
import com.donggree.club.internal.application.ClubSearch;
import com.donggree.club.internal.application.ClubSort;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubDetailReadModel;
import com.donggree.club.internal.application.readmodel.ClubPageReadModel;
import com.donggree.club.internal.application.readmodel.ClubRecruitmentReadModel;
import com.donggree.club.internal.application.readmodel.ClubSummaryReadModel;
import com.donggree.club.internal.domain.ClubLink;
import com.donggree.club.internal.domain.ClubOfficer;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import com.donggree.global.support.RestDocsSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;

class ClubControllerTest extends RestDocsSupport {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final ClubRecruitmentReadModel RECRUITMENT = new ClubRecruitmentReadModel(
            101L,
            25,
            false,
            AT.minusDays(3),
            AT.plusDays(5),
            false,
            List.of(new ClubLink("지원서", "https://forms.gle/donggree")),
            "면접은 3월 둘째 주예요.");

    private final ClubQueryService queryService = Mockito.mock(ClubQueryService.class);

    @Override
    protected Object initController() {
        return new ClubController(queryService);
    }

    @Override
    protected Object[] controllerAdvices() {
        return new Object[] {new GeneralExceptionAdvice()};
    }

    @Test
    void 동아리_목록을_필터_정렬해_커서로_조회한다() throws Exception {
        ClubSearch search = new ClubSearch(
                Set.of(ClubAffiliation.CENTRAL, ClubAffiliation.UNION),
                Set.of(ClubField.PERFORMANCE),
                Set.of(RecruitmentStatus.OPEN),
                ClubSort.DEADLINE,
                null);
        ClubSummaryReadModel club = new ClubSummaryReadModel(
                1L,
                "소리마루",
                ClubAffiliation.CENTRAL,
                List.of(ClubField.PERFORMANCE),
                ClubRegistration.OFFICIAL,
                true,
                List.of(),
                120,
                LocalDate.of(1998, 3, 2),
                RECRUITMENT);
        given(queryService.getClubs(eq(search), eq("MHwwfDF86rCA")))
                .willReturn(new ClubPageReadModel(List.of(club), "MHwwfDJ87IaM", 13));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs")
                        .param("affiliations", "CENTRAL", "UNION")
                        .param("fields", "PERFORMANCE")
                        .param("recruitmentStatuses", "OPEN")
                        .param("sort", "DEADLINE")
                        .param("cursor", "MHwwfDF86rCA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.clubs[0].recruitment.id").value(101))
                .andExpect(jsonPath("$.result.totalCount").value(13))
                .andDo(document(
                        "club-list",
                        queryParameters(
                                parameterWithName("affiliations")
                                        .optional()
                                        .description("소속 분류(CENTRAL·DEPARTMENT·UNION·CAMPUS_ORG). 반복하거나 쉼표로 여러 개"),
                                parameterWithName("fields").optional().description("활동 분야. 하나라도 해당하면 통과"),
                                parameterWithName("recruitmentStatuses")
                                        .optional()
                                        .description("모집 상태(BEFORE·OPEN·CLOSED). 모집 건이 없으면 BEFORE"),
                                parameterWithName("sort")
                                        .optional()
                                        .description("NAME(기본)·DEADLINE(마감 임박)·MEMBERS(인원 많은)·OLDEST(창립 오래된)"),
                                parameterWithName("ids").optional().description("이 ID의 동아리로만 좁힌다(찜 모아보기)"),
                                parameterWithName("cursor").optional().description("이전 묶음의 nextCursor. 첫 묶음은 생략")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("result.clubs[]").description("동아리 최대 12개"),
                                        fieldWithPath("result.nextCursor")
                                                .optional()
                                                .description("다음 묶음 커서(형식은 서버 내부값). 마지막이면 null"),
                                        fieldWithPath("result.totalCount").description("필터를 적용한 전체 개수"))
                                .andWithPrefix("result.clubs[].", summaryFields())
                                .andWithPrefix("result.clubs[].recruitment.", recruitmentFields())));
    }

    @Test
    void 잘못된_커서는_400이다() throws Exception {
        given(queryService.getClubs(eq(new ClubSearch(null, null, null, null, null)), eq("broken")))
                .willThrow(new GeneralException(ClubErrorCode.INVALID_CURSOR));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs").param("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLUB400_1"));
    }

    @Test
    void 알_수_없는_필터_값은_400이다() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs").param("sort", "POPULAR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 동아리_상세를_조회한다() throws Exception {
        ClubDetailReadModel detail = new ClubDetailReadModel(
                1L,
                "소리마루",
                ClubAffiliation.CENTRAL,
                List.of(ClubField.PERFORMANCE),
                ClubRegistration.OFFICIAL,
                true,
                120,
                LocalDate.of(1998, 3, 2),
                null,
                "매주 합주하고 학기마다 정기 공연을 여는 밴드 동아리예요.",
                24,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 2, 28),
                "학생회관 312호",
                14,
                List.of(),
                null,
                RECRUITMENT,
                List.of(new ClubOfficer("회장", "컴퓨터공학과", "김동글")),
                List.of(new ClubLink("인스타그램", "https://instagram.com/donggree")),
                false,
                false,
                AT);
        given(queryService.getClub(1L)).willReturn(detail);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs/{clubId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.recruitment").isEmpty())
                .andExpect(jsonPath("$.result.previousRecruitment.id").value(101))
                .andDo(document(
                        "club-detail",
                        pathParameters(parameterWithName("clubId").description("동아리 ID")),
                        responseFields(
                                        fieldWithPath("isSuccess").description("요청 성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"))
                                .andWithPrefix("result.", detailFields())
                                .andWithPrefix("result.previousRecruitment.", recruitmentFields())));
    }

    @Test
    void 없는_동아리는_404다() throws Exception {
        given(queryService.getClub(9L)).willThrow(new GeneralException(ClubErrorCode.CLUB_NOT_FOUND));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/clubs/{clubId}", 9L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLUB404_1"));
    }

    private static List<FieldDescriptor> commonFields() {
        return List.of(
                fieldWithPath("id").description("동아리 ID"),
                fieldWithPath("name").description("동아리 이름"),
                fieldWithPath("affiliation").description("소속 분류"),
                fieldWithPath("fields[]").description("활동 분야(1~3개, 코드 순)"),
                fieldWithPath("registration").description("PROVISIONAL(관리자 임시 등록)·OFFICIAL(정식 등록)"),
                fieldWithPath("activityConfirmed").description("현재 활동 확인. 기수제는 활동 기간 안, 기수가 없으면 최근 3월·9월 1일 이후 정보 갱신"),
                fieldWithPath("totalMemberCount").optional().description("OB 포함 전체 활동 인원. 미입력이면 null"),
                fieldWithPath("foundedDate").optional().description("창립일. 모르면 null"),
                fieldWithPath("recruitment").optional().description("현재 모집. 모집 건이 없거나, 마감 후 기수 활동이 끝났으면 null(모집 전)"));
    }

    private static List<FieldDescriptor> summaryFields() {
        List<FieldDescriptor> fields = new java.util.ArrayList<>(commonFields());
        fields.add(fieldWithPath("imageUrls[]").description("대표 사진을 맨 앞에 둔 사진 주소(사진 저장 기능 전까지 빈 목록)"));
        return fields;
    }

    private static List<FieldDescriptor> detailFields() {
        List<FieldDescriptor> fields = new java.util.ArrayList<>(commonFields());
        fields.addAll(List.of(
                fieldWithPath("description").description("동아리 소개(100~1000자)"),
                fieldWithPath("generation").optional().description("현재 기수. 기수 없이 활동하면 null"),
                fieldWithPath("activityStartDate").optional().description("현재 기수 활동 시작일"),
                fieldWithPath("activityEndDate").optional().description("현재 기수 활동 종료일"),
                fieldWithPath("roomLocation").optional().description("동아리방 위치. 없으면 null"),
                fieldWithPath("generationMemberCount").optional().description("한 기수 선발 인원"),
                fieldWithPath("images[]").description("사진(사진 저장 기능 전까지 빈 목록)"),
                fieldWithPath("representativeImageId").optional().description("대표 사진 ID"),
                fieldWithPath("previousRecruitment").optional().description("현재 모집이 없을 때 보여줄 가장 최근의 지난 모집. 없으면 null"),
                fieldWithPath("officers[]").description("운영진(최대 5명)"),
                fieldWithPath("officers[].role").description("직책"),
                fieldWithPath("officers[].department").description("학과. 비우면 빈 문자열"),
                fieldWithPath("officers[].name").description("이름"),
                fieldWithPath("links[]").description("동아리 관련 링크(최대 3개)"),
                fieldWithPath("links[].label").description("표시 이름(1~10자)"),
                fieldWithPath("links[].url").description("https 주소"),
                fieldWithPath("canEditDirectly").description("요청 회원이 승인 없이 바로 수정할 수 있는지(요청 승인 API 전까지 false)"),
                fieldWithPath("firstRegistrationRewardAvailable")
                        .description("관리자 임시 등록 동아리라 첫 회원 승인 때 최초 등록 포인트 대상인지"),
                fieldWithPath("updatedAt").description("마지막 정보 등록·수정 시각(한국 시간)")));
        return fields;
    }

    private static List<FieldDescriptor> recruitmentFields() {
        return List.of(
                fieldWithPath("id").description("모집 건 ID"),
                fieldWithPath("generation").optional().description("모집 기수. 없으면 null"),
                fieldWithPath("alwaysOpen").description("상시모집 여부"),
                fieldWithPath("startsAt").optional().description("모집 시작(한국 시간, 포함). 상시모집이면 null"),
                fieldWithPath("endsAt").optional().description("모집 마감(한국 시간, 제외). 상시모집이면 null"),
                fieldWithPath("closedEarly").description("조기 마감 여부. 기간보다 우선"),
                fieldWithPath("links[]").description("모집 관련 링크(최대 3개)"),
                fieldWithPath("links[].label").description("표시 이름"),
                fieldWithPath("links[].url").description("https 주소"),
                fieldWithPath("notice").optional().description("모집 안내(1000자 이내)"));
    }
}
