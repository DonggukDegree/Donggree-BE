package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.readmodel.PlaceReviewPageReadModel;
import com.donggree.place.internal.application.readmodel.PlaceReviewReadModel;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.MemberProfile;
import com.donggree.user.UserLookupService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceReviewQueryServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long PLACE_ID = 10L;

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceReviewRepository reviewRepository = mock(PlaceReviewRepository.class);
    private final PlaceQueries placeQueries = mock(PlaceQueries.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final PlaceReviewQueryService service =
            new PlaceReviewQueryService(placeRepository, reviewRepository, placeQueries, userLookupService);

    @BeforeEach
    void setUp() {
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE))
                .willReturn(Optional.of(PlaceFixtures.place(PLACE_ID, "동국반점", NEAR_CAMPUS)));
        given(userLookupService.findProfiles(any())).willReturn(Map.of(100L, new MemberProfile(100L, "김아코", null)));
    }

    @Test
    void 열한_건을_받아_열_건과_다음_커서를_돌려준다() {
        List<PlaceReview> fetched = LongStream.rangeClosed(1, 11)
                .mapToObj(n -> review(200 - n, 100 + n))
                .toList();
        given(placeQueries.findVisibleReviews(PLACE_ID, null, 11)).willReturn(fetched);

        PlaceReviewPageReadModel page = service.getReviews(MEMBER_ID, PLACE_ID, null);

        assertThat(page.reviews()).hasSize(10);
        assertThat(page.nextCursor()).isEqualTo(190L);
        assertThat(page.myReview()).isNull();
    }

    @Test
    void 마지막_페이지는_커서가_없고_탈퇴_작성자는_닉네임이_null이다() {
        PlaceReview anonymized = review(150, 100);
        ReflectionTestUtils.setField(anonymized, "authorId", null);
        given(placeQueries.findVisibleReviews(PLACE_ID, 160L, 11)).willReturn(List.of(review(155, 100), anonymized));

        PlaceReviewPageReadModel page = service.getReviews(MEMBER_ID, PLACE_ID, 160L);

        assertThat(page.nextCursor()).isNull();
        assertThat(page.reviews())
                .extracting(PlaceReviewReadModel::authorNickname)
                .containsExactly("김아코", null);
    }

    @Test
    void 내_리뷰는_숨겨졌어도_따로_돌려주고_목록에서는_mine으로_표시한다() {
        PlaceReview mine = review(170, MEMBER_ID);
        mine.hide();
        given(placeQueries.findVisibleReviews(PLACE_ID, null, 11)).willReturn(List.of(review(180, MEMBER_ID)));
        given(reviewRepository.findByPlaceIdAndAuthorIdAndDeletedAtIsNull(PLACE_ID, MEMBER_ID))
                .willReturn(Optional.of(mine));

        PlaceReviewPageReadModel page = service.getReviews(MEMBER_ID, PLACE_ID, null);

        assertThat(page.reviews().getFirst().mine()).isTrue();
        assertThat(page.myReview().id()).isEqualTo(170L);
        assertThat(page.myReview().hidden()).isTrue();
    }

    @Test
    void 없거나_숨긴_장소는_404다() {
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReviews(MEMBER_ID, PLACE_ID, null)).isInstanceOf(GeneralException.class);
    }

    private static PlaceReview review(long id, long authorId) {
        PlaceReview review = PlaceReview.write(PLACE_ID, authorId, 4, "짜장면이 맛있고 양이 많아요");
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }
}
