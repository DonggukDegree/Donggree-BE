package com.donggree.club.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.club.internal.application.command.ClubSaveCommand;
import com.donggree.club.internal.application.command.ClubSaveResult;
import com.donggree.club.internal.application.command.RecruitmentSaveCommand;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.apiPayload.code.BaseErrorCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.UserLookupService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ClubCommandServiceTest {

    private static final Long MEMBER = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);

    private final ClubRepository clubs = mock(ClubRepository.class);
    private final ClubRecruitmentRepository recruitments = mock(ClubRecruitmentRepository.class);
    private final ClubRequestRepository requests = mock(ClubRequestRepository.class);
    private final ClubEditorRepository editors = mock(ClubEditorRepository.class);
    private final UserLookupService users = mock(UserLookupService.class);
    private final ClubCommandService service =
            new ClubCommandService(clubs, recruitments, requests, editors, users, () -> NOW);

    private Club club;

    @BeforeEach
    void setUp() {
        given(users.isStudentVerified(MEMBER)).willReturn(true);
        club = Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.OFFICIAL, NOW.minusDays(10));
        ReflectionTestUtils.setField(club, "id", 1L);
        given(clubs.findById(1L)).willReturn(Optional.of(club));
    }

    @Test
    void 학생_인증이_없으면_요청할_수_없다() {
        given(users.isStudentVerified(MEMBER)).willReturn(false);

        assertError(() -> service.register(MEMBER, info("소리마루")), ClubErrorCode.VERIFICATION_REQUIRED);
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void 신규_등록은_요청자_정보와_함께_승인_요청으로_남긴다() {
        ClubSaveResult result = service.register(MEMBER, info("별자리 탐험대"));

        ClubRequest saved = savedRequest();
        assertThat(result).isEqualTo(ClubSaveResult.pending(null));
        assertThat(saved.getType()).isEqualTo(ClubRequestType.REGISTER);
        assertThat(saved.getAfter().imageCount()).isEqualTo(2);
        assertThat(saved.getRequesterContact()).isEqualTo("dongle@dgu.ac.kr");
    }

    @Test
    void 입력_규칙을_어기면_400이다() {
        ClubSaveCommand shortDescription = new ClubSaveCommand(
                "소리마루",
                null,
                List.of(),
                "짧아요",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                "dongle@dgu.ac.kr",
                ClubRequesterRole.STAFF);

        assertError(() -> service.register(MEMBER, shortDescription), ClubErrorCode.INVALID_CLUB);
        assertError(
                () -> service.register(MEMBER, withRequester(null, ClubRequesterRole.STAFF)),
                ClubErrorCode.INVALID_REQUESTER);
    }

    @Test
    void 직접_수정_권한이_있으면_요청자_정보_없이_바로_반영한다() {
        given(editors.canEdit(1L, MEMBER)).willReturn(true);

        ClubSaveResult result = service.edit(MEMBER, 1L, withRequester(null, null));

        assertThat(result).isEqualTo(ClubSaveResult.direct(1L));
        assertThat(club.getName()).isEqualTo("소리마루 밴드");
        assertThat(club.getInfoUpdatedAt()).isEqualTo(NOW);
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void 권한이_없으면_현재_값과_함께_수정_요청을_남긴다() {
        ClubSaveResult result = service.edit(MEMBER, 1L, info("소리마루 밴드"));

        ClubRequest saved = savedRequest();
        assertThat(result).isEqualTo(ClubSaveResult.pending(1L));
        assertThat(saved.getBefore().profile().name()).isEqualTo("소리마루");
        assertThat(saved.getAfter().profile().name()).isEqualTo("소리마루 밴드");
        assertThat(club.getName()).isEqualTo("소리마루");
    }

    @Test
    void 같은_동아리_같은_종류의_검토_중_요청이_있으면_409다() {
        given(requests.existsByRequesterIdAndClubIdAndTypeAndStatus(
                        MEMBER, 1L, ClubRequestType.EDIT, ClubRequestStatus.PENDING))
                .willReturn(true);

        assertError(() -> service.edit(MEMBER, 1L, info("소리마루 밴드")), ClubErrorCode.DUPLICATE_PENDING);
    }

    @Test
    void 권한_회원의_모집은_진행_중이면_고쳐_쓰고_마감됐으면_새로_만든다() {
        given(editors.canEdit(1L, MEMBER)).willReturn(true);
        ClubRecruitment ongoing = ClubRecruitment.open(1L, ClubFixtures.period(NOW.minusDays(1), NOW.plusDays(5)));
        given(recruitments.findTopByClubIdOrderByIdDesc(1L)).willReturn(Optional.of(ongoing));

        service.saveRecruitment(MEMBER, 1L, alwaysOpen());

        assertThat(ongoing.isAlwaysOpen()).isTrue();
        verify(recruitments, never()).save(any());

        ClubRecruitment closed = ClubRecruitment.open(1L, ClubFixtures.period(NOW.minusDays(9), NOW.minusDays(2)));
        given(recruitments.findTopByClubIdOrderByIdDesc(1L)).willReturn(Optional.of(closed));

        service.saveRecruitment(MEMBER, 1L, alwaysOpen());

        verify(recruitments).save(any(ClubRecruitment.class));
        assertThat(closed.isAlwaysOpen()).isFalse();
    }

    @Test
    void 권한이_없는_모집_요청은_마감된_모집을_변경_전_값으로_두지_않는다() {
        ClubRecruitment closed = ClubRecruitment.open(1L, ClubFixtures.period(NOW.minusDays(9), NOW.minusDays(2)));
        given(recruitments.findTopByClubIdOrderByIdDesc(1L)).willReturn(Optional.of(closed));

        service.saveRecruitment(MEMBER, 1L, alwaysOpen());

        ClubRequest saved = savedRequest();
        assertThat(saved.getType()).isEqualTo(ClubRequestType.RECRUITMENT);
        assertThat(saved.getBefore()).isNull();
        assertThat(saved.getAfter().recruitment().schedule().alwaysOpen()).isTrue();
    }

    @Test
    void 검토_중_요청을_다시_보내면_취소하고_새_요청을_남긴다() {
        ClubRequest pending = ClubRequest.edit(
                club,
                MEMBER,
                requester(),
                ClubRequestPayload.of(club.profile(), 0),
                ClubRequestPayload.of(ClubFixtures.profile("소리마루 밴드"), 0));
        given(requests.findById(30L)).willReturn(Optional.of(pending));

        ClubSaveResult result = service.resubmitInfo(MEMBER, 30L, info("소리마루 합주단"));

        assertThat(result).isEqualTo(ClubSaveResult.pending(1L));
        assertThat(pending.getStatus()).isEqualTo(ClubRequestStatus.CANCELED);
        assertThat(savedRequest().getAfter().profile().name()).isEqualTo("소리마루 합주단");
    }

    @Test
    void 다른_회원_요청은_404이고_종류가_다르면_400이다() {
        ClubRequest others =
                ClubRequest.register(99L, requester(), ClubRequestPayload.of(ClubFixtures.profile("가"), 0));
        ClubRequest register =
                ClubRequest.register(MEMBER, requester(), ClubRequestPayload.of(ClubFixtures.profile("가"), 0));
        given(requests.findById(1L)).willReturn(Optional.of(others));
        given(requests.findById(2L)).willReturn(Optional.of(register));

        assertError(() -> service.resubmitInfo(MEMBER, 1L, info("가")), ClubErrorCode.REQUEST_NOT_FOUND);
        assertError(() -> service.resubmitRecruitment(MEMBER, 2L, alwaysOpen()), ClubErrorCode.REQUEST_TYPE_MISMATCH);
    }

    @Test
    void 취소된_요청은_다시_보낼_수_없다() {
        ClubRequest canceled =
                ClubRequest.register(MEMBER, requester(), ClubRequestPayload.of(ClubFixtures.profile("가"), 0));
        canceled.cancelForResubmit(NOW);
        given(requests.findById(3L)).willReturn(Optional.of(canceled));

        assertError(() -> service.resubmitInfo(MEMBER, 3L, info("가")), ClubErrorCode.REQUEST_CLOSED);
    }

    private ClubRequest savedRequest() {
        ArgumentCaptor<ClubRequest> captor = ArgumentCaptor.forClass(ClubRequest.class);
        verify(requests).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static ClubRequester requester() {
        return new ClubRequester(ClubRequesterRole.PRESIDENT, "dongle@dgu.ac.kr");
    }

    private static ClubSaveCommand info(String name) {
        return command(name, "dongle@dgu.ac.kr", ClubRequesterRole.PRESIDENT);
    }

    private static ClubSaveCommand withRequester(String contact, ClubRequesterRole role) {
        return command("소리마루 밴드", contact, role);
    }

    private static ClubSaveCommand command(String name, String contact, ClubRequesterRole role) {
        var profile = ClubFixtures.profile(name);
        return new ClubSaveCommand(
                profile.name(),
                profile.affiliation(),
                profile.fields(),
                profile.description(),
                profile.generation(),
                profile.activityStartDate(),
                profile.activityEndDate(),
                profile.roomLocation(),
                profile.generationMemberCount(),
                profile.totalMemberCount(),
                profile.foundedDate(),
                List.of(new ClubSaveCommand.Officer("회장", "", "김동글")),
                List.of(new ClubSaveCommand.Link("인스타그램", "https://instagram.com/donggree")),
                2,
                contact,
                role);
    }

    private static RecruitmentSaveCommand alwaysOpen() {
        return new RecruitmentSaveCommand(
                null, true, null, null, false, List.of(), null, "010-1234-5678", ClubRequesterRole.STAFF);
    }

    private static void assertError(Executable executable, BaseErrorCode code) {
        assertThatThrownBy(executable::execute)
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(code));
    }
}
