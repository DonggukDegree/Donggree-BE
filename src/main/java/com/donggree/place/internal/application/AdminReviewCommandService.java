package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReportRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자의 리뷰 숨김·숨김 해제. 장소와 같은 규칙: 숨기면 대기 신고 처리 완료, 해제하면 대기 신고 기각.
 * 통합 신고 대시보드 전까지 신고 확인은 DB와 디스코드 알림으로 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminReviewCommandService {

    private final PlaceReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;

    @Transactional
    public void hide(Long reviewId) {
        find(reviewId).hide();
        reportRepository.resolvePending(reviewId, LocalDateTime.now());
    }

    @Transactional
    public void show(Long reviewId) {
        find(reviewId).show();
        reportRepository.rejectPending(reviewId, LocalDateTime.now());
    }

    private PlaceReview find(Long reviewId) {
        return reviewRepository
                .findById(reviewId)
                .filter(review -> !review.isDeleted())
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REVIEW_NOT_FOUND));
    }
}
