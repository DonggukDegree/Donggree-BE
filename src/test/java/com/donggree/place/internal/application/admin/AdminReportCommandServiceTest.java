package com.donggree.place.internal.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReport;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspendMemberCommand;
import com.donggree.user.SuspensionSource;
import com.donggree.user.UserModerationService;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminReportCommandServiceTest {

    private final PlaceReportRepository placeReportRepository = mock(PlaceReportRepository.class);
    private final ReviewReportRepository reviewReportRepository = mock(ReviewReportRepository.class);
    private final PlaceReviewRepository reviewRepository = mock(PlaceReviewRepository.class);
    private final UserModerationService moderationService = mock(UserModerationService.class);
    private final AdminReportCommandService service = new AdminReportCommandService(
            placeReportRepository, reviewReportRepository, reviewRepository, moderationService);
    private PlaceReport placeReport;
    private ReviewReport reviewReport;
    private PlaceReview review;

    @BeforeEach
    void setUp() {
        placeReport = PlaceReport.submit(10L, 8L, PlaceReportReason.INAPPROPRIATE, null);
        given(placeReportRepository.findById(1L)).willReturn(Optional.of(placeReport));
        reviewReport = ReviewReport.submit(30L, 8L, ReviewReportReason.ABUSE, null);
        given(reviewReportRepository.findById(2L)).willReturn(Optional.of(reviewReport));
        review = PlaceReview.write(10L, 7L, 1, "별로였어요 다시는 안 가요");
        ReflectionTestUtils.setField(review, "id", 30L);
        given(reviewRepository.findById(30L)).willReturn(Optional.of(review));
    }

    @Test
    void 대기_신고를_처리_완료하거나_기각하고_이미_처리한_신고는_409_7이다() {
        service.resolvePlaceReport(1L);
        assertThat(placeReport.getStatus()).isEqualTo(PlaceReportStatus.RESOLVED);
        assertError(() -> service.rejectPlaceReport(1L), PlaceErrorCode.REPORT_ALREADY_PROCESSED);

        service.rejectReviewReport(2L);
        assertThat(reviewReport.getStatus()).isEqualTo(PlaceReportStatus.REJECTED);
        assertThat(reviewReport.getProcessedAt()).isNotNull();
        assertError(() -> service.resolveReviewReport(2L), PlaceErrorCode.REPORT_ALREADY_PROCESSED);
        assertError(() -> service.resolvePlaceReport(99L), PlaceErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void 리뷰_신고로_작성자를_정지하면_신고_근거를_남기고_신고를_처리_완료한다() {
        service.suspendByReviewReport(2L, 30, "욕설", 1L);

        verify(moderationService)
                .suspend(new SuspendMemberCommand(
                        7L, 30, SuspensionSource.REPORT, ReportTargetType.REVIEW, 2L, "욕설", 1L));
        assertThat(reviewReport.getStatus()).isEqualTo(PlaceReportStatus.RESOLVED);
    }

    @Test
    void 장소_신고로_고른_회원을_정지하고_작성자가_없으면_409_8이다() {
        service.suspendByPlaceReport(1L, 5L, 7, null, 1L);
        verify(moderationService)
                .suspend(
                        new SuspendMemberCommand(5L, 7, SuspensionSource.REPORT, ReportTargetType.PLACE, 1L, null, 1L));
        assertThat(placeReport.getStatus()).isEqualTo(PlaceReportStatus.RESOLVED);

        ReflectionTestUtils.setField(review, "authorId", null);
        assertError(() -> service.suspendByReviewReport(2L, 3, null, 1L), PlaceErrorCode.SUSPENSION_TARGET_MISSING);
        assertError(
                () -> service.suspendByPlaceReport(1L, null, 3, null, 1L), PlaceErrorCode.SUSPENSION_TARGET_MISSING);
    }

    @Test
    void 정지가_실패하면_신고는_대기로_남는다() {
        given(moderationService.suspend(any()))
                .willThrow(new GeneralException(com.donggree.global.apiPayload.code.GeneralErrorCode.VALID_FAIL));

        assertThatThrownBy(() -> service.suspendByReviewReport(2L, 0, null, 1L)).isInstanceOf(GeneralException.class);
        assertThat(reviewReport.isPending()).isTrue();
        verify(placeReportRepository, never()).findById(any());
    }

    private static void assertError(ThrowingCallable call, PlaceErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(code));
    }
}
