package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.WriteReviewCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceReviewCommandServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long PLACE_ID = 10L;
    private static final WriteReviewCommand GOOD = new WriteReviewCommand(4, "짜장면이 맛있고 양이 많아요");

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceReviewRepository reviewRepository = mock(PlaceReviewRepository.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final PlaceReviewCommandService service =
            new PlaceReviewCommandService(placeRepository, reviewRepository, userLookupService);

    @BeforeEach
    void setUp() {
        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(true);
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE))
                .willReturn(Optional.of(PlaceFixtures.place(PLACE_ID, "동국반점", NEAR_CAMPUS)));
        given(reviewRepository.saveAndFlush(any(PlaceReview.class))).willAnswer(invocation -> {
            PlaceReview review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 30L);
            return review;
        });
    }

    @Test
    void 인증_회원은_장소마다_리뷰를_한_건_작성한다() {
        assertThat(service.write(MEMBER_ID, PLACE_ID, GOOD)).isEqualTo(30L);
    }

    @Test
    void 작성_조건을_어기면_막는다() {
        given(reviewRepository.existsByPlaceIdAndAuthorIdAndDeletedAtIsNull(PLACE_ID, MEMBER_ID))
                .willReturn(true);
        assertError(() -> service.write(MEMBER_ID, PLACE_ID, GOOD), PlaceErrorCode.DUPLICATE_REVIEW);

        given(reviewRepository.existsByPlaceIdAndAuthorIdAndDeletedAtIsNull(PLACE_ID, MEMBER_ID))
                .willReturn(false);
        assertError(
                () -> service.write(MEMBER_ID, PLACE_ID, new WriteReviewCommand(4, "짧아요")),
                PlaceErrorCode.INVALID_REVIEW);

        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE)).willReturn(Optional.empty());
        assertError(() -> service.write(MEMBER_ID, PLACE_ID, GOOD), PlaceErrorCode.PLACE_NOT_FOUND);

        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(false);
        assertError(() -> service.write(MEMBER_ID, PLACE_ID, GOOD), PlaceErrorCode.VERIFICATION_REQUIRED);
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_두_번_작성해_유일_인덱스에_걸리면_409_4다() {
        given(reviewRepository.saveAndFlush(any(PlaceReview.class)))
                .willThrow(new DataIntegrityViolationException("uk_place_review_author"));

        assertError(() -> service.write(MEMBER_ID, PLACE_ID, GOOD), PlaceErrorCode.DUPLICATE_REVIEW);
    }

    @Test
    void 본인_리뷰만_수정하고_숨겨진_리뷰는_수정할_수_없다() {
        PlaceReview mine = stored(MEMBER_ID);
        service.edit(MEMBER_ID, PLACE_ID, 30L, new WriteReviewCommand(5, "탕수육도 바삭하고 맛있어요"));
        assertThat(mine.getRating()).isEqualTo(5);

        assertError(() -> service.edit(8L, PLACE_ID, 30L, GOOD), PlaceErrorCode.VERIFICATION_REQUIRED);
        given(userLookupService.isStudentVerified(8L)).willReturn(true);
        assertError(() -> service.edit(8L, PLACE_ID, 30L, GOOD), PlaceErrorCode.NOT_REVIEW_AUTHOR);

        mine.hide();
        assertError(() -> service.edit(MEMBER_ID, PLACE_ID, 30L, GOOD), PlaceErrorCode.REVIEW_HIDDEN);
        assertError(() -> service.edit(MEMBER_ID, PLACE_ID, 99L, GOOD), PlaceErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void 본인은_숨겨진_리뷰도_지울_수_있고_행은_남긴다() {
        PlaceReview mine = stored(MEMBER_ID);
        mine.hide();

        assertError(() -> service.delete(8L, PLACE_ID, 30L), PlaceErrorCode.NOT_REVIEW_AUTHOR);
        service.delete(MEMBER_ID, PLACE_ID, 30L);

        assertThat(mine.isDeleted()).isTrue();
        verify(reviewRepository, never()).delete(any());
    }

    private PlaceReview stored(Long authorId) {
        PlaceReview review = PlaceReview.write(PLACE_ID, authorId, 4, "짜장면이 맛있고 양이 많아요");
        ReflectionTestUtils.setField(review, "id", 30L);
        given(reviewRepository.findByIdAndPlaceIdAndDeletedAtIsNull(30L, PLACE_ID))
                .willReturn(Optional.of(review));
        return review;
    }

    private static void assertError(ThrowingCallable call, PlaceErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(code));
    }
}
