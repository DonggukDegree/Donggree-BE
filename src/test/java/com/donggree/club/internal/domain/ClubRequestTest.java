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
    private static final ClubRequestPayload INFO = ClubRequestPayload.of(ClubFixtures.profile("소리마루"), ClubPhotos.NONE);

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
        assertThatThrownBy(() -> new ClubPhotos(java.util.List.of(1L, 2L, 3L, 4L), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClubPhotos(java.util.List.of(1L, 2L), 9L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ClubPhotos(java.util.List.of(5L, 6L, 5L), null))
                .isEqualTo(new ClubPhotos(java.util.List.of(5L, 6L), 5L));
        assertThat(ClubRequestPayload.of(ClubFixtures.alwaysOpen()).photos()).isNull();
        assertThat(ClubRequestPayload.of(ClubFixtures.profile("소리마루"), null).photos())
                .isEqualTo(ClubPhotos.NONE);
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
    void 승인하면_신규_등록은_만든_동아리를_연결하고_처리된_요청은_다시_처리할_수_없다() {
        ClubRequest request = ClubRequest.register(7L, REQUESTER, INFO);

        assertThatThrownBy(() -> request.approve(null, NOW)).isInstanceOf(IllegalArgumentException.class);
        request.approve(3L, NOW);

        assertThat(request.getStatus()).isEqualTo(ClubRequestStatus.APPROVED);
        assertThat(request.getClubId()).isEqualTo(3L);
        assertThat(request.getProcessedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> request.reject("다시", NOW)).isInstanceOf(IllegalStateException.class);
        assertThat(request.resubmittable()).isFalse();
    }

    @Test
    void 반려는_사유가_1자에서_200자다() {
        ClubRequest request = ClubRequest.register(7L, REQUESTER, INFO);

        assertThatThrownBy(() -> request.reject("  ", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> request.reject("가".repeat(201), NOW)).isInstanceOf(IllegalArgumentException.class);
        request.reject(" 활동 사진을 올려주세요. ", NOW);

        assertThat(request.getStatus()).isEqualTo(ClubRequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("활동 사진을 올려주세요.");
        assertThat(request.resubmittable()).isTrue();
    }

    @Test
    void 임시_등록_동아리는_처음_승인될_때만_정식으로_바뀐다() {
        Club provisional = Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.PROVISIONAL, NOW);

        assertThat(provisional.becomeOfficial()).isTrue();
        assertThat(provisional.becomeOfficial()).isFalse();
        assertThat(provisional.getRegistration()).isEqualTo(ClubRegistration.OFFICIAL);
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
