package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.MemberSuspensionRecord;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspendMemberCommand;
import com.donggree.user.SuspensionSource;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.domain.MemberSuspension;
import com.donggree.user.internal.domain.MemberSuspensionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserModerationServiceImplTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberSuspensionRepository suspensionRepository = mock(MemberSuspensionRepository.class);
    private final UserModerationServiceImpl service =
            new UserModerationServiceImpl(memberRepository, suspensionRepository);
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.registerKakaoMember("oauth", "test@example.invalid");
        ReflectionTestUtils.setField(member, "id", 7L);
        member.updateRefreshToken("refresh");
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(suspensionRepository.save(any(MemberSuspension.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 신고로_정지하면_지금부터_일수만큼_막고_기존_로그인을_끊는다() {
        MemberSuspensionRecord record = service.suspend(
                new SuspendMemberCommand(7L, 30, SuspensionSource.REPORT, ReportTargetType.REVIEW, 60L, "욕설", 1L));

        assertThat(record.days()).isEqualTo(30);
        assertThat(record.reportType()).isEqualTo(ReportTargetType.REVIEW);
        assertThat(member.getSuspendedUntil()).isEqualTo(record.endsAt());
        assertThat(member.getRefreshToken()).isNull();
    }

    @Test
    void 정지_중_다시_정지하면_새_일수로_바꾸고_이전_정지는_해제로_남긴다() {
        MemberSuspension previous = MemberSuspension.start(
                7L,
                30,
                SuspensionSource.ADMIN,
                null,
                null,
                null,
                1L,
                LocalDateTime.now().minusDays(1));
        given(suspensionRepository.findTopByMemberIdOrderByIdDesc(7L)).willReturn(Optional.of(previous));

        MemberSuspensionRecord record =
                service.suspend(new SuspendMemberCommand(7L, 3, SuspensionSource.ADMIN, null, null, null, 2L));

        assertThat(previous.getLiftedAt()).isNotNull();
        assertThat(previous.getLiftedBy()).isEqualTo(2L);
        assertThat(member.getSuspendedUntil()).isEqualTo(record.endsAt());
        assertThat(record.endsAt()).isBefore(LocalDateTime.now().plusDays(4));
    }

    @Test
    void 정지_중이면_언제든_해제하고_해제_시각을_이력에_남긴다() {
        MemberSuspension active = MemberSuspension.start(
                7L,
                30,
                SuspensionSource.ADMIN,
                null,
                null,
                null,
                1L,
                LocalDateTime.now().minusHours(1));
        member.suspendUntil(active.getEndsAt());
        given(suspensionRepository.findTopByMemberIdOrderByIdDesc(7L)).willReturn(Optional.of(active));

        service.lift(7L, 2L);

        assertThat(member.getSuspendedUntil()).isNull();
        assertThat(active.getLiftedBy()).isEqualTo(2L);
    }

    @Test
    void 정지_중이면_종료_시각_일수_사유_구분을_알려준다() {
        LocalDateTime until = LocalDateTime.now().plusDays(10);
        given(memberRepository.findSuspendedUntilById(7L)).willReturn(Optional.of(until));
        given(suspensionRepository.findTopByMemberIdOrderByIdDesc(7L))
                .willReturn(Optional.of(MemberSuspension.start(
                        7L, 14, SuspensionSource.REPORT, ReportTargetType.PLACE, 3L, null, 1L, LocalDateTime.now())));

        var restriction = service.findRestriction(7L).orElseThrow();
        assertThat(restriction.suspendedUntil()).isEqualTo(until);
        assertThat(restriction.days()).isEqualTo(14);
        assertThat(restriction.byReport()).isTrue();

        given(memberRepository.findSuspendedUntilById(7L))
                .willReturn(Optional.of(LocalDateTime.now().minusMinutes(1)));
        assertThat(service.findRestriction(7L)).isEmpty();
        given(memberRepository.findSuspendedUntilById(8L)).willReturn(Optional.empty());
        assertThat(service.findRestriction(8L)).isEmpty();
    }

    @Test
    void 잘못된_일수는_400_없는_회원은_404다() {
        assertThatThrownBy(() ->
                        service.suspend(new SuspendMemberCommand(7L, 0, SuspensionSource.ADMIN, null, null, null, 1L)))
                .isInstanceOf(GeneralException.class)
                .satisfies(
                        e -> assertThat(((GeneralException) e).getCode()).isEqualTo(UserErrorCode.INVALID_SUSPENSION));
        assertThatThrownBy(() ->
                        service.suspend(new SuspendMemberCommand(9L, 3, SuspensionSource.ADMIN, null, null, null, 1L)))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(UserErrorCode.MEMBER_NOT_FOUND));
    }
}
