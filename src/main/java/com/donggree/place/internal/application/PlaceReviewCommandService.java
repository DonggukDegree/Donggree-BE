package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.WriteReviewCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 리뷰 작성·수정·삭제 응용 서비스.
 * 작성·수정은 인증 회원만, 수정·삭제는 본인만 한다. 회원당 장소별 1건이며 삭제하면 다시 쓸 수 있다.
 */
@Service
@RequiredArgsConstructor
public class PlaceReviewCommandService {

    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository reviewRepository;
    private final UserLookupService userLookupService;

    /** 동시에 두 번 작성해도 DB 부분 유일 인덱스(장소·작성자·미삭제)가 한 건만 남긴다. */
    @Transactional
    public Long write(Long memberId, Long placeId, WriteReviewCommand command) {
        requireStudentVerified(memberId);
        requireVisiblePlace(placeId);
        if (reviewRepository.existsByPlaceIdAndAuthorIdAndDeletedAtIsNull(placeId, memberId)) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REVIEW);
        }
        PlaceReview review;
        try {
            review = PlaceReview.write(placeId, memberId, command.rating(), command.content());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_REVIEW);
        }
        try {
            return reviewRepository.saveAndFlush(review).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REVIEW);
        }
    }

    /** 숨겨진 리뷰는 고쳐서 다시 드러내지 못하게 막는다(삭제는 가능). */
    @Transactional
    public void edit(Long memberId, Long placeId, Long reviewId, WriteReviewCommand command) {
        requireStudentVerified(memberId);
        requireVisiblePlace(placeId);
        PlaceReview review = ownReview(memberId, placeId, reviewId);
        if (!review.isVisible()) {
            throw new GeneralException(PlaceErrorCode.REVIEW_HIDDEN);
        }
        try {
            review.edit(command.rating(), command.content());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_REVIEW);
        }
    }

    /** 행은 남기고 삭제 시각만 기록한다(신고 이력 보존). 인증 여부와 무관하게 본인이면 지울 수 있다. */
    @Transactional
    public void delete(Long memberId, Long placeId, Long reviewId) {
        requireVisiblePlace(placeId);
        ownReview(memberId, placeId, reviewId).delete(LocalDateTime.now());
    }

    private PlaceReview ownReview(Long memberId, Long placeId, Long reviewId) {
        PlaceReview review = reviewRepository
                .findByIdAndPlaceIdAndDeletedAtIsNull(reviewId, placeId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REVIEW_NOT_FOUND));
        if (!review.isAuthor(memberId)) {
            throw new GeneralException(PlaceErrorCode.NOT_REVIEW_AUTHOR);
        }
        return review;
    }

    private void requireVisiblePlace(Long placeId) {
        if (placeRepository.findByIdAndStatus(placeId, PlaceStatus.VISIBLE).isEmpty()) {
            throw new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND);
        }
    }

    private void requireStudentVerified(Long memberId) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED);
        }
    }
}
