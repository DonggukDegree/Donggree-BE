package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspensionSource;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MemberSuspensionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 10, 0);

    @Test
    void 지금부터_일수만큼_정지하고_신고_정지는_근거_신고를_남긴다() {
        MemberSuspension byReport =
                MemberSuspension.start(7L, 30, SuspensionSource.REPORT, ReportTargetType.REVIEW, 60L, " 욕설 ", 1L, NOW);

        assertThat(byReport.getEndsAt()).isEqualTo(NOW.plusDays(30));
        assertThat(byReport.getNote()).isEqualTo("욕설");
        assertThat(byReport.isActive(NOW)).isTrue();
        assertThat(byReport.isActive(NOW.plusDays(30))).isFalse();

        MemberSuspension byAdmin =
                MemberSuspension.start(7L, 3, SuspensionSource.ADMIN, ReportTargetType.REVIEW, 60L, null, 1L, NOW);
        assertThat(byAdmin.getReportType()).isNull();
        assertThat(byAdmin.getReportId()).isNull();
    }

    @Test
    void 일수는_1일_이상이며_업무_상한은_없고_날짜_범위를_넘는_값만_막는다() {
        assertThatThrownBy(() -> MemberSuspension.start(7L, 0, SuspensionSource.ADMIN, null, null, null, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(MemberSuspension.start(7L, 3650, SuspensionSource.ADMIN, null, null, null, 1L, NOW)
                        .getEndsAt())
                .isEqualTo(NOW.plusDays(3650));
        assertThatThrownBy(() -> MemberSuspension.start(
                        7L, Integer.MAX_VALUE, SuspensionSource.ADMIN, null, null, null, 1L, NOW))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> MemberSuspension.start(7L, 3, SuspensionSource.REPORT, null, null, null, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        MemberSuspension.start(7L, 3, SuspensionSource.ADMIN, null, null, "가".repeat(201), 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 회원은_정지_종료_시각_전까지_정지_중이고_재활성화해도_정지가_유지된다() {
        Member member = Member.registerKakaoMember("oauth", "test@example.invalid");
        member.updateRefreshToken("refresh");
        member.suspendUntil(NOW.plusDays(3));

        assertThat(member.isSuspended(NOW)).isTrue();
        assertThat(member.isSuspended(NOW.plusDays(3))).isFalse();
        assertThat(member.getRefreshToken()).isNull();

        member.withdraw();
        member.reactivate();
        assertThat(member.getSuspendedUntil()).isEqualTo(NOW.plusDays(3));

        member.liftSuspension();
        assertThat(member.isSuspended(NOW)).isFalse();
    }
}
