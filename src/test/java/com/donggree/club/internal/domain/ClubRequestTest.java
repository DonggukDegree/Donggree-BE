package com.donggree.club.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ClubRequestTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final ClubRequester REQUESTER = new ClubRequester(ClubRequesterRole.PRESIDENT, " dongle@dgu.ac.kr ");
    private static final ClubRequestPayload INFO = ClubRequestPayload.of(ClubFixtures.profile("소리마루"), 2);

    @Test
    void 확인_연락처는_이메일_또는_전화번호이고_관계는_필수다() {
        assertThat(REQUESTER.contact()).isEqualTo("dongle@dgu.ac.kr");
        assertThat(new ClubRequester(ClubRequesterRole.STAFF, "01012345678").contact())
                .isEqualTo("01012345678");
        assertThatThrownBy(() -> new ClubRequester(ClubRequesterRole.STAFF, "카톡 아이디"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClubRequester(null, "dongle@dgu.ac.kr"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 요청_값은_동아리_정보와_모집_정보_중_하나만_담는다() {
        assertThatThrownBy(() -> new ClubRequestPayload(null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClubRequestPayload.of(ClubFixtures.profile("소리마루"), 4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ClubRequestPayload.of(ClubFixtures.alwaysOpen()).imageCount())
                .isNull();
    }

    @Test
    void 신규_등록_요청은_요청한_이름과_요청자_정보를_남긴다() {
        ClubRequest request = ClubRequest.register(7L, REQUESTER, INFO);

        assertThat(request.getType()).isEqualTo(ClubRequestType.REGISTER);
        assertThat(request.getClubId()).isNull();
        assertThat(request.getClubName()).isEqualTo("소리마루");
        assertThat(request.getStatus()).isEqualTo(ClubRequestStatus.PENDING);
        assertThat(request.getRequesterRole()).isEqualTo(ClubRequesterRole.PRESIDENT);
        assertThat(request.isRequestedBy(7L)).isTrue();
        assertThat(request.isRequestedBy(8L)).isFalse();
    }

    @Test
    void 요청_종류와_값이_맞지_않으면_거절한다() {
        Club club = Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.OFFICIAL, NOW);

        assertThatThrownBy(() -> ClubRequest.recruitment(club, 7L, REQUESTER, null, INFO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 검토_중_요청은_다시_요청하면_취소되고_반려_요청은_그대로다() {
        ClubRequest pending = ClubRequest.register(7L, REQUESTER, INFO);

        pending.cancelForResubmit(NOW);

        assertThat(pending.getStatus()).isEqualTo(ClubRequestStatus.CANCELED);
        assertThat(pending.getProcessedAt()).isEqualTo(NOW);
        assertThat(pending.resubmittable()).isFalse();
        assertThatThrownBy(() -> pending.cancelForResubmit(NOW)).isInstanceOf(IllegalStateException.class);
    }
}
