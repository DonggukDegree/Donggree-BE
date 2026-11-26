package com.donggree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import com.donggree.club.internal.application.admin.AdminClubCommandService;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.point.internal.application.PointRewardService;
import com.donggree.point.internal.domain.PointHistoryRepository;
import com.donggree.point.internal.domain.PointReason;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** 관리자 동아리 승인과 포인트 모듈 사이의 동시 커밋·롤백·동아리(·기수)별 1회 적립 검증. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.jpa.show-sql=false")
class ClubPointRewardIntegrationTest {

    private static final ClubRequester REQUESTER = new ClubRequester(ClubRequesterRole.PRESIDENT, "dongle@dgu.ac.kr");

    @Autowired
    AdminClubCommandService admin;

    @Autowired
    ClubRequestRepository requests;

    @Autowired
    ClubRepository clubs;

    @Autowired
    ClubRecruitmentRepository recruitments;

    @Autowired
    ClubEditorRepository editors;

    @Autowired
    PointHistoryRepository histories;

    @MockitoSpyBean
    PointRewardService rewards;

    @AfterEach
    void clean() {
        histories.deleteAll();
        requests.deleteAll();
        editors.deleteAll();
        recruitments.deleteAll();
        clubs.deleteAll();
    }

    @Test
    void 신규_등록_승인과_최초_등록_500P를_함께_저장하고_권한을_준다() {
        Long requestId = requests.save(register(7L, "별자리 탐험대")).getId();

        admin.approve(requestId);

        ClubRequest approved = requests.findById(requestId).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(ClubRequestStatus.APPROVED);
        assertThat(editors.canEdit(approved.getClubId(), 7L)).isTrue();
        assertThat(rewardsOf(7L))
                .containsExactly(new Reward(PointReason.CLUB_FIRST_REGISTRATION, 500, approved.getClubId()));
    }

    @Test
    void 같은_기수_모집은_다른_회원이_다시_올려도_한_번만_적립한다() {
        Club club =
                clubs.save(Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.OFFICIAL, LocalDateTime.now()));
        LocalDateTime now = LocalDateTime.now();
        Long first = requests.save(recruitment(club, 8L, now.minusDays(9), now.minusDays(2)))
                .getId();
        admin.approve(first);
        Long second = requests.save(recruitment(club, 9L, now.minusDays(1), now.plusDays(5)))
                .getId();

        admin.approve(second);

        assertThat(rewardsOf(8L))
                .containsExactly(new Reward(PointReason.CLUB_RECRUITMENT, 100, club.getId() * 10000L + 25));
        assertThat(rewardsOf(9L)).isEmpty();
        assertThat(recruitments
                        .findTopByClubIdOrderByIdDesc(club.getId())
                        .orElseThrow()
                        .getStartsAt())
                .isEqualTo(now.minusDays(1));
    }

    @Test
    void 적립에_실패하면_승인도_롤백돼_다시_승인할_수_있다() {
        Long requestId = requests.save(register(7L, "별자리 탐험대")).getId();
        doThrow(new IllegalStateException("적립 실패"))
                .when(rewards)
                .rewardClubApproval(anyLong(), anyLong(), anyBoolean(), any(), any());

        assertThatThrownBy(() -> admin.approve(requestId)).isInstanceOf(IllegalStateException.class);

        assertThat(requests.findById(requestId).orElseThrow().getStatus()).isEqualTo(ClubRequestStatus.PENDING);
        assertThat(clubs.count()).isZero();
        assertThat(histories.count()).isZero();
    }

    private List<Reward> rewardsOf(Long memberId) {
        return histories.findAll().stream()
                .filter(history -> history.getMemberId().equals(memberId))
                .map(history -> new Reward(history.getReason(), history.getAmount(), history.getTargetId()))
                .toList();
    }

    private static ClubRequest register(Long memberId, String name) {
        return ClubRequest.register(memberId, REQUESTER, ClubRequestPayload.of(ClubFixtures.profile(name), 0));
    }

    private static ClubRequest recruitment(Club club, Long memberId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return ClubRequest.recruitment(
                club, memberId, REQUESTER, null, ClubRequestPayload.of(ClubFixtures.period(startsAt, endsAt)));
    }

    record Reward(PointReason reason, int amount, Long targetId) {}
}
