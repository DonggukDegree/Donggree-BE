package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.ReportReviewCommand;
import com.donggree.place.internal.application.event.ReviewReportedEvent;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReport;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import com.donggree.user.UserLookupService;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class ReviewReportCommandServiceTest {

    private static final Long REPORTER_ID = 8L;
    private static final Long PLACE_ID = 10L;
    private static final Long REVIEW_ID = 30L;
    private static final ReportReviewCommand ABUSE = new ReportReviewCommand(ReviewReportReason.ABUSE, null);

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceReviewRepository reviewRepository = mock(PlaceReviewRepository.class);
    private final ReviewReportRepository reportRepository = mock(ReviewReportRepository.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ReviewReportCommandService service = new ReviewReportCommandService(
            placeRepository, reviewRepository, reportRepository, userLookupService, eventPublisher);
    private PlaceReview review;

    @BeforeEach
    void setUp() {
        given(userLookupService.isStudentVerified(REPORTER_ID)).willReturn(true);
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE))
                .willReturn(Optional.of(PlaceFixtures.place(PLACE_ID, "동국반점", NEAR_CAMPUS)));
        review = PlaceReview.write(PLACE_ID, 7L, 1, "가".repeat(120));
        ReflectionTestUtils.setField(review, "id", REVIEW_ID);
        given(reviewRepository.findByIdAndPlaceIdAndDeletedAtIsNull(REVIEW_ID, PLACE_ID))
                .willReturn(Optional.of(review));
        given(reportRepository.saveAndFlush(any(ReviewReport.class))).willAnswer(invocation -> {
            ReviewReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 60L);
            return report;
        });
        given(reportRepository.countByReviewIdAndStatus(REVIEW_ID, PlaceReportStatus.PENDING))
                .willReturn(1L);
    }

    @Test
    void 다른_회원의_리뷰를_신고하고_본문_앞부분과_함께_알림_이벤트를_낸다() {
        assertThat(service.report(REPORTER_ID, PLACE_ID, REVIEW_ID, ABUSE)).isEqualTo(60L);

        assertThat(review.isVisible()).isTrue();
        verify(eventPublisher)
                .publishEvent(new ReviewReportedEvent(
                        60L,
                        REVIEW_ID,
                        PLACE_ID,
                        "동국반점",
                        1,
                        "가".repeat(100) + "…",
                        ReviewReportReason.ABUSE,
                        null,
                        1L,
                        false));
    }

    @Test
    void 서로_다른_회원의_처리_대기_신고가_3건이_되면_리뷰를_자동_숨긴다() {
        given(reportRepository.countByReviewIdAndStatus(REVIEW_ID, PlaceReportStatus.PENDING))
                .willReturn(3L);

        service.report(REPORTER_ID, PLACE_ID, REVIEW_ID, ABUSE);

        assertThat(review.isVisible()).isFalse();
    }

    @Test
    void 신고_조건을_어기면_막는다() {
        assertError(() -> service.report(7L, PLACE_ID, REVIEW_ID, ABUSE), PlaceErrorCode.VERIFICATION_REQUIRED);
        given(userLookupService.isStudentVerified(7L)).willReturn(true);
        assertError(() -> service.report(7L, PLACE_ID, REVIEW_ID, ABUSE), PlaceErrorCode.SELF_REVIEW_REPORT);

        given(reportRepository.existsByReviewIdAndReporterIdAndStatus(
                        REVIEW_ID, REPORTER_ID, PlaceReportStatus.PENDING))
                .willReturn(true);
        assertError(
                () -> service.report(REPORTER_ID, PLACE_ID, REVIEW_ID, ABUSE), PlaceErrorCode.DUPLICATE_REVIEW_REPORT);

        given(reportRepository.existsByReviewIdAndReporterIdAndStatus(
                        REVIEW_ID, REPORTER_ID, PlaceReportStatus.PENDING))
                .willReturn(false);
        assertError(
                () -> service.report(
                        REPORTER_ID, PLACE_ID, REVIEW_ID, new ReportReviewCommand(ReviewReportReason.OTHER, " ")),
                PlaceErrorCode.INVALID_REPORT);

        review.hide();
        assertError(() -> service.report(REPORTER_ID, PLACE_ID, REVIEW_ID, ABUSE), PlaceErrorCode.REVIEW_NOT_FOUND);
        verify(reportRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 관리자_숨김과_해제는_대기_신고를_처리_완료와_기각으로_바꾼다() {
        AdminReviewCommandService admin = new AdminReviewCommandService(reviewRepository, reportRepository);
        given(reviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

        admin.hide(REVIEW_ID);
        assertThat(review.isVisible()).isFalse();
        verify(reportRepository).resolvePending(eq(REVIEW_ID), any());

        admin.show(REVIEW_ID);
        assertThat(review.isVisible()).isTrue();
        verify(reportRepository).rejectPending(eq(REVIEW_ID), any());

        given(reviewRepository.findById(99L)).willReturn(Optional.empty());
        assertError(() -> admin.hide(99L), PlaceErrorCode.REVIEW_NOT_FOUND);
    }

    private static void assertError(ThrowingCallable call, PlaceErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(code));
    }
}
