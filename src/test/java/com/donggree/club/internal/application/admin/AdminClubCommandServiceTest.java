package com.donggree.club.internal.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.donggree.club.event.ClubRequestApprovedEvent;
import com.donggree.club.internal.application.ClubPhotoBinder;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditor;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubPhotos;
import com.donggree.club.internal.domain.ClubProfile;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class AdminClubCommandServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final ClubRequester REQUESTER = new ClubRequester(ClubRequesterRole.PRESIDENT, "dongle@dgu.ac.kr");

    private final ClubRequestRepository requests = mock(ClubRequestRepository.class);
    private final ClubRepository clubs = mock(ClubRepository.class);
    private final ClubRecruitmentRepository recruitments = mock(ClubRecruitmentRepository.class);
    private final ClubEditorRepository editors = mock(ClubEditorRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ClubPhotoBinder photos = mock(ClubPhotoBinder.class);
    private final AdminClubCommandService service =
            new AdminClubCommandService(requests, clubs, recruitments, editors, photos, events, () -> NOW);

    private Club club;

    @BeforeEach
    void setUp() {
        club = Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.PROVISIONAL, NOW.minusDays(5));
        ReflectionTestUtils.setField(club, "id", 3L);
        given(clubs.findById(3L)).willReturn(Optional.of(club));
        given(clubs.save(any(Club.class))).willAnswer(invocation -> {
            Club saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
    }

    @Test
    void 신규_등록_승인은_정식_동아리를_만들고_권한과_최초_등록_적립을_준다() {
        ClubRequest request = pending(ClubRequest.register(7L, REQUESTER, info("별자리 탐험대")));

        service.approve(1L);

        assertThat(request.getStatus()).isEqualTo(ClubRequestStatus.APPROVED);
        assertThat(request.getClubId()).isEqualTo(10L);
        ArgumentCaptor<Club> created = ArgumentCaptor.forClass(Club.class);
        verify(clubs).save(created.capture());
        assertThat(created.getValue().getRegistration()).isEqualTo(ClubRegistration.OFFICIAL);
        verify(editors).save(any(ClubEditor.class));
        verify(photos).bind(10L, ClubPhotos.NONE);
        assertThat(publishedEvent()).isEqualTo(new ClubRequestApprovedEvent(7L, 10L, true, null, null));
    }

    @Test
    void 임시_등록_동아리_수정_승인은_정식_전환과_기수_갱신_적립을_준다() {
        ClubProfile base = ClubFixtures.profile("소리마루");
        ClubProfile nextGeneration = new ClubProfile(
                base.name(),
                base.affiliation(),
                base.fields(),
                base.description(),
                25,
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2028, 2, 28),
                base.roomLocation(),
                base.generationMemberCount(),
                base.totalMemberCount(),
                base.foundedDate(),
                base.officers(),
                base.links());
        pending(ClubRequest.edit(
                club,
                7L,
                REQUESTER,
                ClubRequestPayload.of(base, ClubPhotos.NONE),
                ClubRequestPayload.of(nextGeneration, ClubPhotos.NONE)));

        service.approve(1L);

        assertThat(club.getRegistration()).isEqualTo(ClubRegistration.OFFICIAL);
        assertThat(club.getGeneration()).isEqualTo(25);
        assertThat(publishedEvent()).isEqualTo(new ClubRequestApprovedEvent(7L, 3L, true, null, 25));
    }

    @Test
    void 처음_모집하는_기수의_새_모집만_모집_적립_대상이다() {
        club.becomeOfficial();
        pending(ClubRequest.recruitment(
                club,
                7L,
                REQUESTER,
                null,
                ClubRequestPayload.of(ClubFixtures.period(NOW.plusDays(1), NOW.plusDays(8)))));
        given(recruitments.findTopByClubIdOrderByIdDesc(3L)).willReturn(Optional.empty());
        given(recruitments.existsByClubIdAndGeneration(3L, 25)).willReturn(false);

        service.approve(1L);

        verify(recruitments).save(any(ClubRecruitment.class));
        assertThat(publishedEvent()).isEqualTo(new ClubRequestApprovedEvent(7L, 3L, false, 25, null));
    }

    @Test
    void 이미_모집한_기수나_진행_중_모집_수정은_적립하지_않는다() {
        club.becomeOfficial();
        pending(ClubRequest.recruitment(
                club,
                7L,
                REQUESTER,
                null,
                ClubRequestPayload.of(ClubFixtures.period(NOW.plusDays(1), NOW.plusDays(8)))));
        given(recruitments.findTopByClubIdOrderByIdDesc(3L)).willReturn(Optional.empty());
        given(recruitments.existsByClubIdAndGeneration(3L, 25)).willReturn(true);
        given(editors.canEdit(3L, 7L)).willReturn(true);

        service.approve(1L);

        verify(events, never()).publishEvent(any());
        verify(editors, never()).save(any());
    }

    @Test
    void 탈퇴한_요청자의_요청은_반영만_하고_권한과_적립은_없다() {
        ClubRequest request = pending(ClubRequest.register(7L, REQUESTER, info("별자리 탐험대")));
        ReflectionTestUtils.setField(request, "requesterId", null);

        service.approve(1L);

        assertThat(request.getStatus()).isEqualTo(ClubRequestStatus.APPROVED);
        verify(editors, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void 처리된_요청과_빈_반려_사유는_거절한다() {
        ClubRequest request = pending(ClubRequest.register(7L, REQUESTER, info("별자리 탐험대")));

        assertThatThrownBy(() -> service.reject(1L, " "))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.INVALID_REJECT_REASON));
        service.reject(1L, "활동 사진을 올려주세요.");
        assertThat(request.getStatus()).isEqualTo(ClubRequestStatus.REJECTED);
        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.REQUEST_ALREADY_PROCESSED));
    }

    @Test
    void 관리자_임시_등록과_수정은_승인_없이_반영한다() {
        assertThat(service.registerProvisional(ClubCommands.info("한수바둑"))).isEqualTo(10L);

        service.edit(3L, ClubCommands.info("소리마루 밴드"));

        assertThat(club.getName()).isEqualTo("소리마루 밴드");
        assertThat(club.getRegistration()).isEqualTo(ClubRegistration.PROVISIONAL);
    }

    private ClubRequest pending(ClubRequest request) {
        given(requests.findByIdForUpdate(1L)).willReturn(Optional.of(request));
        return request;
    }

    private ClubRequestApprovedEvent publishedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        return (ClubRequestApprovedEvent) captor.getValue();
    }

    private static ClubRequestPayload info(String name) {
        return ClubRequestPayload.of(ClubFixtures.profile(name), ClubPhotos.NONE);
    }
}
