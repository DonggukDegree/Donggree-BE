package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.ReportReviewCommand;
import com.donggree.place.internal.application.event.ReviewReportedEvent;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReport;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리뷰 신고 응용 서비스. 장소 신고와 같은 규칙으로 서로 다른 회원 3명의 처리 대기 신고가 쌓이면 리뷰를 자동 숨기고,
 * 그 밖의 처리는 관리자가 한다. 접수가 커밋되면 운영 알림 이벤트를 발행한다.
 */
@Service
@RequiredArgsConstructor
public class ReviewReportCommandService {

    static final int EXCERPT_MAX_LENGTH = 100;

    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long report(Long memberId, Long placeId, Long reviewId, ReportReviewCommand command) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED);
        }
        Place place = placeRepository
                .findByIdAndStatus(placeId, PlaceStatus.VISIBLE)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
        PlaceReview review = reviewRepository
                .findByIdAndPlaceIdAndDeletedAtIsNull(reviewId, placeId)
                .filter(PlaceReview::isVisible)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REVIEW_NOT_FOUND));
        if (review.isAuthor(memberId)) {
            throw new GeneralException(PlaceErrorCode.SELF_REVIEW_REPORT);
        }
        if (reportRepository.existsByReviewIdAndReporterIdAndStatus(reviewId, memberId, PlaceReportStatus.PENDING)) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REVIEW_REPORT);
        }
        ReviewReport report;
        try {
            report = ReviewReport.submit(reviewId, memberId, command.reason(), command.description());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_REPORT);
        }
        Long reportId;
        try {
            reportId = reportRepository.saveAndFlush(report).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REVIEW_REPORT);
        }
        long pendingCount = reportRepository.countByReviewIdAndStatus(reviewId, PlaceReportStatus.PENDING);
        boolean autoHidden = pendingCount >= ReviewReport.AUTO_HIDE_PENDING_COUNT;
        if (autoHidden) {
            review.hide();
        }
        eventPublisher.publishEvent(new ReviewReportedEvent(
                reportId,
                reviewId,
                placeId,
                place.getName(),
                review.getRating(),
                excerpt(review.getContent()),
                report.getReason(),
                report.getDescription(),
                pendingCount,
                autoHidden));
        return reportId;
    }

    private static String excerpt(String content) {
        return content.length() <= EXCERPT_MAX_LENGTH ? content : content.substring(0, EXCERPT_MAX_LENGTH) + "…";
    }
}
