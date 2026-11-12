package com.donggree.place.internal.infrastructure;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import com.donggree.place.internal.application.PlaceQueries;
import com.donggree.place.internal.application.readmodel.RatingSummary;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReport;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, QuerydslPlaceQueries.class})
class PlaceReviewPersistenceTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reportRepository;

    @Autowired
    private QuerydslPlaceQueries placeQueries;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 평균_별점과_리뷰_수는_숨기거나_지운_리뷰를_빼고_소수_첫째_자리로_센다() {
        Place place = savePlace("동국반점");
        Place empty = savePlace("필동커피");
        review(place, 1L, 5);
        review(place, 2L, 4);
        review(place, 3L, 4);
        review(place, 4L, 1).hide();
        review(place, 5L, 1).delete(LocalDateTime.now());
        entityManager.flush();

        assertThat(placeQueries.findRatingSummary(place.getId())).isEqualTo(new RatingSummary(4.3, 3));
        assertThat(placeQueries.findRatingSummary(empty.getId())).isEqualTo(RatingSummary.EMPTY);
        assertThat(placeQueries.findRatingSummaries())
                .containsOnlyKeys(place.getId())
                .containsEntry(place.getId(), new RatingSummary(4.3, 3));
    }

    @Test
    void 목록_리뷰는_숨긴_리뷰를_포함하고_지운_리뷰만_빼서_최신순으로_커서_이전부터_조회한다() {
        Place place = savePlace("동국반점");
        PlaceReview first = review(place, 1L, 5);
        review(place, 2L, 4).delete(LocalDateTime.now());
        PlaceReview hidden = review(place, 3L, 3);
        hidden.hide();
        PlaceReview fourth = review(place, 4L, 2);
        entityManager.flush();

        assertThat(placeQueries.findListedReviews(place.getId(), null, 2))
                .extracting(PlaceReview::getId)
                .containsExactly(fourth.getId(), hidden.getId());
        assertThat(placeQueries.findListedReviews(place.getId(), hidden.getId(), 2))
                .extracting(PlaceReview::getId)
                .containsExactly(first.getId());
    }

    @Test
    void 관리자_리뷰_목록은_상태와_본문_장소이름으로_거르고_대기_신고_수를_센다() {
        Place place = savePlace("동국반점");
        Place other = savePlace("필동커피");
        PlaceReview visible = review(place, 1L, 5);
        PlaceReview hidden = review(place, 2L, 1);
        hidden.hide();
        PlaceReview deleted = review(other, 3L, 3);
        deleted.delete(LocalDateTime.now());
        reportRepository.save(ReviewReport.submit(hidden.getId(), 8L, ReviewReportReason.ABUSE, null));
        reportRepository.save(ReviewReport.submit(hidden.getId(), 9L, ReviewReportReason.SPAM, null));
        entityManager.flush();
        var page = org.springframework.data.domain.PageRequest.of(0, 20);

        assertThat(placeQueries.findReviewsForAdmin(null, null, page).getContent())
                .extracting(PlaceReview::getId)
                .containsExactly(deleted.getId(), hidden.getId(), visible.getId());
        assertThat(placeQueries
                        .findReviewsForAdmin(PlaceQueries.AdminReviewState.VISIBLE, null, page)
                        .getContent())
                .extracting(PlaceReview::getId)
                .containsExactly(visible.getId());
        assertThat(placeQueries
                        .findReviewsForAdmin(PlaceQueries.AdminReviewState.HIDDEN, null, page)
                        .getContent())
                .extracting(PlaceReview::getId)
                .containsExactly(hidden.getId());
        assertThat(placeQueries
                        .findReviewsForAdmin(PlaceQueries.AdminReviewState.DELETED, null, page)
                        .getContent())
                .extracting(PlaceReview::getId)
                .containsExactly(deleted.getId());
        assertThat(placeQueries.findReviewsForAdmin(null, "필동", page).getContent())
                .extracting(PlaceReview::getId)
                .containsExactly(deleted.getId());
        var hiddenOnly = placeQueries.findReviewsForAdmin(PlaceQueries.AdminReviewState.HIDDEN, null, page);
        assertThat(hiddenOnly.getTotalElements()).isEqualTo(1);
        assertThat(placeQueries.countPendingReviewReports(List.of(visible.getId(), hidden.getId())))
                .containsExactly(java.util.Map.entry(hidden.getId(), 2L));
        assertThat(placeQueries.countPendingReviewReports(List.of())).isEmpty();
    }

    @Test
    void 관리자_장소_목록은_상태와_이름으로_거르고_최신_등록순이다() {
        Place first = savePlace("동국반점");
        Place second = savePlace("필동커피");
        second.hide();
        entityManager.flush();
        var page = org.springframework.data.domain.PageRequest.of(0, 20);

        assertThat(placeQueries.findPlacesForAdmin(null, null, page).getContent())
                .extracting(Place::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(placeQueries
                        .findPlacesForAdmin(com.donggree.place.internal.domain.enums.PlaceStatus.HIDDEN, null, page)
                        .getContent())
                .extracting(Place::getId)
                .containsExactly(second.getId());
        assertThat(placeQueries.findPlacesForAdmin(null, "반점", page).getContent())
                .extracting(Place::getId)
                .containsExactly(first.getId());
        assertThat(placeQueries.countPendingPlaceReports(List.of(first.getId())))
                .isEmpty();
    }

    @Test
    void 회원당_리뷰_확인은_지운_리뷰를_빼고_탈퇴하면_작성자_연결만_끊는다() {
        Place place = savePlace("동국반점");
        PlaceReview review = review(place, 7L, 5);
        entityManager.flush();
        assertThat(reviewRepository.existsByPlaceIdAndAuthorIdAndDeletedAtIsNull(place.getId(), 7L))
                .isTrue();

        assertThat(reviewRepository.clearAuthor(7L)).isEqualTo(1);
        PlaceReview found = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(found.getAuthorId()).isNull();
        assertThat(found.getContent()).isEqualTo("맛있고 친절한 가게예요!");

        PlaceReview again = review(place, 8L, 3);
        again.delete(LocalDateTime.now());
        entityManager.flush();
        assertThat(reviewRepository.existsByPlaceIdAndAuthorIdAndDeletedAtIsNull(place.getId(), 8L))
                .isFalse();
    }

    @Test
    void 리뷰_신고_대기_수와_처리_완료_기각과_신고자_해제() {
        Place place = savePlace("동국반점");
        PlaceReview review = review(place, 7L, 1);
        reportRepository.save(ReviewReport.submit(review.getId(), 8L, ReviewReportReason.ABUSE, null));
        reportRepository.save(ReviewReport.submit(review.getId(), 9L, ReviewReportReason.SPAM, null));
        entityManager.flush();

        assertThat(reportRepository.countByReviewIdAndStatus(review.getId(), PlaceReportStatus.PENDING))
                .isEqualTo(2);
        assertThat(reportRepository.resolvePending(review.getId(), LocalDateTime.now()))
                .isEqualTo(2);
        assertThat(reportRepository.rejectPending(review.getId(), LocalDateTime.now()))
                .isZero();
        assertThat(reportRepository.clearReporter(8L)).isEqualTo(1);
        assertThat(reportRepository.findByReviewIdOrderByIdAsc(review.getId()))
                .allSatisfy(report -> assertThat(report.getStatus()).isEqualTo(PlaceReportStatus.RESOLVED));
    }

    private Place savePlace(String name) {
        Place place = placeRepository.save(Place.register(PlaceFixtures.registration(name, NEAR_CAMPUS)));
        entityManager.flush();
        return place;
    }

    private PlaceReview review(Place place, Long authorId, int rating) {
        return reviewRepository.saveAndFlush(PlaceReview.write(place.getId(), authorId, rating, "맛있고 친절한 가게예요!"));
    }

    @Test
    void 관리자_목록은_상태별_최신순이고_작성자별로_받은_리뷰_신고를_센다() {
        Place place = savePlace("동국반점");
        PlaceReview first = review(place, 7L, 1);
        PlaceReview second = review(place, 8L, 2);
        ReviewReport pending =
                reportRepository.save(ReviewReport.submit(first.getId(), 9L, ReviewReportReason.ABUSE, null));
        ReviewReport resolved = ReviewReport.submit(first.getId(), 10L, ReviewReportReason.SPAM, null);
        resolved.resolve(LocalDateTime.now());
        reportRepository.save(resolved);
        reportRepository.save(ReviewReport.submit(second.getId(), 9L, ReviewReportReason.PRIVACY, null));
        entityManager.flush();

        var pendingPage = reportRepository.findForAdmin(
                PlaceReportStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(
                        0,
                        20,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "id")));
        assertThat(pendingPage.getTotalElements()).isEqualTo(2);
        assertThat(pendingPage.getContent().getLast().getId()).isEqualTo(pending.getId());
        assertThat(reportRepository
                        .findForAdmin(null, org.springframework.data.domain.PageRequest.of(0, 20))
                        .getTotalElements())
                .isEqualTo(3);
        assertThat(reportRepository.countReceivedByAuthorIds(java.util.List.of(7L, 8L, 11L)))
                .extracting(row -> row[0] + ":" + row[1])
                .containsExactlyInAnyOrder("7:2", "8:1");
    }
}
