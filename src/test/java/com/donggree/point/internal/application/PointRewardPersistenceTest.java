package com.donggree.point.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.point.internal.application.readmodel.PointHistoryPageReadModel;
import com.donggree.point.internal.domain.PointHistoryRepository;
import com.donggree.point.internal.domain.PointReason;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({PointRewardService.class, PointQueryService.class})
class PointRewardPersistenceTest {

    private static final Long A = 1L;
    private static final Long B = 2L;
    private static final Long PLACE = 10L;

    @Autowired
    private PointRewardService rewardService;

    @Autowired
    private PointQueryService queryService;

    @Autowired
    private PointHistoryRepository historyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 추리_정답은_회원과_문제_날짜별_한번만_10P를_적립하고_안내한다() {
        var day = LocalDate.of(2026, 11, 24);
        rewardService.rewardMysterySolved(A, day);
        rewardService.rewardMysterySolved(A, day);
        rewardService.rewardMysterySolved(A, day.plusDays(1));
        rewardService.rewardMysterySolved(B, day);

        assertThat(queryService.getTotalPoints(A)).isEqualTo(20);
        assertThat(queryService.getTotalPoints(B)).isEqualTo(10);
        assertThat(rewardService.acknowledge(A, PointReason.MYSTERY_SOLVED, 20261124L))
                .contains(10);
        assertThat(rewardService.acknowledge(A, PointReason.MYSTERY_SOLVED, 20261124L))
                .isEmpty();
        assertThat(rewardService.acknowledge(A, PointReason.MYSTERY_SOLVED, 20261126L))
                .isEmpty();
    }

    @Test
    void 졸업_리포트_최초_확인은_회원당_한_번만_적립한다() {
        rewardService.rewardReportFirstView(A);
        rewardService.rewardReportFirstView(A);
        rewardService.rewardReportFirstView(B);

        assertThat(queryService.getTotalPoints(A)).isEqualTo(500);
        assertThat(queryService.getTotalPoints(B)).isEqualTo(500);
        assertThat(historyRepository.count()).isEqualTo(2);
    }

    @Test
    void 장소_등록은_장소마다_정보_수정은_회원당_장소별_한_번_적립한다() {
        rewardService.rewardPlaceRegister(A, PLACE);
        rewardService.rewardPlaceRegister(A, PLACE);
        rewardService.rewardPlaceRegister(A, 11L);
        rewardService.rewardInfoEdit(A, PLACE);
        rewardService.rewardInfoEdit(A, PLACE);
        rewardService.rewardInfoEdit(A, 11L);
        rewardService.rewardInfoEdit(B, PLACE);

        assertThat(queryService.getTotalPoints(A)).isEqualTo(500 + 500 + 100 + 100);
        assertThat(queryService.getTotalPoints(B)).isEqualTo(100);
    }

    @Test
    void 첫_리뷰는_장소별_한_명만_500P이고_리뷰_적립은_다시_써도_회원당_장소별_한_번이다() {
        rewardService.rewardReview(A, PLACE, true);
        // 동시에 쓰여 둘 다 첫 리뷰로 판단돼도 두 번째 회원은 일반 리뷰로 받는다.
        rewardService.rewardReview(B, PLACE, true);
        // 지웠다 다시 쓴 리뷰
        rewardService.rewardReview(A, PLACE, false);
        rewardService.rewardReview(B, PLACE, false);

        assertThat(queryService.getTotalPoints(A)).isEqualTo(500);
        assertThat(queryService.getTotalPoints(B)).isEqualTo(100);
        assertThat(queryService.getHistories(A, null).histories())
                .extracting(PointHistoryPageReadModel.Item::reason)
                .containsExactly(PointReason.PLACE_FIRST_REVIEW);
    }

    @Test
    void 장소가_숨겨지면_등록_적립만_회수하고_해제하면_되살린다() {
        rewardService.rewardPlaceRegister(A, PLACE);
        rewardService.rewardInfoEdit(B, PLACE);

        rewardService.onPlaceVisibilityChanged(PLACE, false);
        rewardService.onPlaceVisibilityChanged(PLACE, false);
        assertThat(queryService.getTotalPoints(A)).isZero();
        assertThat(queryService.getTotalPoints(B)).isEqualTo(100);
        assertThat(queryService.getHistories(A, null).histories())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.revoked()).isTrue();
                    assertThat(item.amount()).isEqualTo(500);
                });

        rewardService.onPlaceVisibilityChanged(PLACE, true);
        assertThat(queryService.getTotalPoints(A)).isEqualTo(500);
    }

    @Test
    void 리뷰가_숨겨지면_작성자의_그_장소_리뷰_적립만_회수하고_해제하면_되살린다() {
        rewardService.rewardReview(A, PLACE, true);
        rewardService.rewardReview(B, PLACE, false);
        rewardService.rewardReview(A, 11L, false);

        rewardService.onReviewVisibilityChanged(A, PLACE, false);
        rewardService.onReviewVisibilityChanged(null, PLACE, false);
        assertThat(queryService.getTotalPoints(A)).isEqualTo(100);
        assertThat(queryService.getTotalPoints(B)).isEqualTo(100);

        rewardService.onReviewVisibilityChanged(A, PLACE, true);
        assertThat(queryService.getTotalPoints(A)).isEqualTo(600);
    }

    @Test
    void 적립_내역은_최신순으로_20개씩_커서로_넘긴다() {
        IntStream.rangeClosed(1, 25).forEach(placeId -> rewardService.rewardPlaceRegister(A, (long) placeId));
        rewardService.rewardPlaceRegister(B, 99L);

        PointHistoryPageReadModel first = queryService.getHistories(A, null);
        assertThat(first.histories()).hasSize(20);
        assertThat(first.nextCursor()).isNotNull();
        PointHistoryPageReadModel second = queryService.getHistories(A, first.nextCursor());
        assertThat(second.histories()).hasSize(5);
        assertThat(second.nextCursor()).isNull();
        assertThat(first.histories().getFirst().historyId())
                .isGreaterThan(second.histories().getFirst().historyId());
        assertThat(second.histories().getLast().historyId()).isLessThan(first.nextCursor());
    }

    @Test
    void 적립_안내는_적립이_있고_아직_안내하지_않았을_때_한_번만_한다() {
        rewardService.rewardInfoEdit(A, PLACE);
        rewardService.rewardReportFirstView(A);

        assertThat(rewardService.acknowledge(A, PointReason.PLACE_INFO_EDIT, PLACE))
                .contains(100);
        assertThat(rewardService.acknowledge(A, PointReason.PLACE_INFO_EDIT, PLACE))
                .isEmpty();
        assertThat(rewardService.acknowledge(A, PointReason.PLACE_INFO_EDIT, 11L))
                .isEmpty();
        assertThat(rewardService.acknowledge(B, PointReason.PLACE_INFO_EDIT, PLACE))
                .isEmpty();
        assertThat(rewardService.acknowledge(A, PointReason.GRADUATION_REPORT_FIRST_VIEW, null))
                .contains(500);
        assertThat(rewardService.acknowledge(A, PointReason.GRADUATION_REPORT_FIRST_VIEW, null))
                .isEmpty();
    }

    @Test
    void 회수된_적립은_안내하지_않는다() {
        rewardService.rewardPlaceRegister(A, PLACE);
        rewardService.onPlaceVisibilityChanged(PLACE, false);

        assertThat(rewardService.acknowledge(A, PointReason.PLACE_REGISTER, PLACE))
                .isEmpty();
    }

    @Test
    void 탈퇴하면_그_회원의_내역만_지운다() {
        rewardService.rewardPlaceRegister(A, PLACE);
        rewardService.rewardReview(B, PLACE, true);

        rewardService.deleteAllOfMember(A);

        assertThat(queryService.getTotalPoints(A)).isZero();
        assertThat(queryService.getHistories(A, null).histories()).isEmpty();
        assertThat(queryService.getTotalPoints(B)).isEqualTo(500);
    }

    @Test
    void 리포트_적립에만_대상이_없고_금액은_양수여야_한다() {
        assertThatThrownBy(() -> {
                    historyRepository.grant(A, "PLACE_REGISTER", 500, null, LocalDateTime.now());
                    entityManager.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 금액이_0_이하인_적립은_DB에서_막는다() {
        assertThatThrownBy(() -> historyRepository.grant(A, "PLACE_REVIEW", 0, PLACE, LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
