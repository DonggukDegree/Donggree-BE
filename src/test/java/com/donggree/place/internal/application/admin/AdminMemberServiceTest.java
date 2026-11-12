package com.donggree.place.internal.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.user.MemberAdminProfile;
import com.donggree.user.MemberSearchPage;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspendMemberCommand;
import com.donggree.user.SuspensionSource;
import com.donggree.user.UserModerationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminMemberServiceTest {

    private final UserModerationService moderationService = mock(UserModerationService.class);
    private final TranscriptLookupService transcriptLookupService = mock(TranscriptLookupService.class);
    private final CurriculumLookupService curriculumLookupService = mock(CurriculumLookupService.class);
    private final ReviewReportRepository reviewReportRepository = mock(ReviewReportRepository.class);
    private final AdminMemberService service = new AdminMemberService(
            moderationService,
            new AdminMemberReader(moderationService, transcriptLookupService, curriculumLookupService),
            reviewReportRepository);
    private final MemberAdminProfile profile =
            new MemberAdminProfile(7L, "김아코", "김아코", "2026000001", "STUDENT", LocalDateTime.of(2026, 3, 2, 9, 0), null);

    @BeforeEach
    void setUp() {
        given(moderationService.findAdminProfiles(List.of(7L))).willReturn(Map.of(7L, profile));
        given(transcriptLookupService.findDepartmentIdsByMemberIds(List.of(7L))).willReturn(Map.of(7L, 3L));
        given(curriculumLookupService.findDepartmentNamesByIds(List.of(3L))).willReturn(Map.of(3L, "컴퓨터공학전공"));
        given(curriculumLookupService.findCollegeNamesByDepartmentIds(List.of(3L)))
                .willReturn(Map.of(3L, "AI융합대학"));
        List<Object[]> reviewCounts = new ArrayList<>();
        reviewCounts.add(new Object[] {7L, 4L});
        given(reviewReportRepository.countReceivedByAuthorIds(List.of(7L))).willReturn(reviewCounts);
        given(moderationService.countReportSuspensions(List.of(7L), ReportTargetType.PLACE))
                .willReturn(Map.of(7L, 1L));
    }

    @Test
    void 회원_상세에_이름_학번_단과대_학과와_받은_신고_합계를_담는다() {
        var detail = service.getMember(7L);

        assertThat(detail.member())
                .isEqualTo(new AdminMemberSummary(7L, "김아코", "김아코", "2026000001", "AI융합대학", "컴퓨터공학전공", null));
        assertThat(detail.receivedReviewReportCount()).isEqualTo(4);
        assertThat(detail.placeReportSuspensionCount()).isEqualTo(1);
        assertThat(detail.receivedReportCount()).isEqualTo(5);
    }

    @Test
    void 회원_검색_행에도_학과와_받은_신고_수를_붙이고_성적표가_없으면_학과는_비운다() {
        given(moderationService.searchMembers("김", 0, 20)).willReturn(new MemberSearchPage(List.of(profile), 1, 0, 20));
        given(transcriptLookupService.findDepartmentIdsByMemberIds(List.of(7L))).willReturn(Map.of());

        var page = service.searchMembers("김", 0);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().member().departmentName()).isNull();
        assertThat(page.items().getFirst().receivedReportCount()).isEqualTo(5);
    }

    @Test
    void 관리자_정지는_신고_근거_없이_남기고_없는_회원_상세는_404_5다() {
        service.suspend(7L, 3, "운영 정책 위반", 1L);
        verify(moderationService)
                .suspend(new SuspendMemberCommand(7L, 3, SuspensionSource.ADMIN, null, null, "운영 정책 위반", 1L));

        given(moderationService.findAdminProfiles(any())).willReturn(Map.of());
        assertThatThrownBy(() -> service.getMember(9L)).isInstanceOf(GeneralException.class);
    }
}
