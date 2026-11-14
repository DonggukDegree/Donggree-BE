package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.application.readmodel.PlaceDuplicatesReadModel;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.application.readmodel.RatingSummary;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import com.donggree.user.MemberProfile;
import com.donggree.user.UserLookupService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaceQueryServiceTest {

    private final PlaceQueries placeQueries = mock(PlaceQueries.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final PlaceQueryService service = new PlaceQueryService(
            placeQueries,
            placeRepository,
            new PlaceDuplicateFinder(placeRepository),
            new PlaceDetailReader(userLookupService, placeQueries));

    @Test
    void 노출_장소의_상세와_메뉴와_등록자_닉네임을_반환한다() {
        given(placeRepository.findByIdAndStatus(1L, PlaceStatus.VISIBLE))
                .willReturn(Optional.of(PlaceFixtures.place(1L, "동국반점", NEAR_CAMPUS)));
        given(userLookupService.findProfiles(List.of(1L))).willReturn(Map.of(1L, new MemberProfile(1L, "김아코", null)));
        given(placeQueries.findRatingSummary(1L)).willReturn(RatingSummary.EMPTY);

        PlaceDetailReadModel detail = service.getPlace(1L);

        assertThat(detail.name()).isEqualTo("동국반점");
        assertThat(detail.registrantNickname()).isEqualTo("김아코");
        assertThat(detail.menus())
                .containsExactly(
                        new PlaceDetailReadModel.Menu("짜장면", 7000), new PlaceDetailReadModel.Menu("탕수육", null));
    }

    @Test
    void 지도_목록에_장소별_평균_별점과_리뷰_수와_태그를_붙이고_없으면_null과_0과_빈_목록이다() {
        given(placeQueries.findVisibleSummaries())
                .willReturn(List.of(
                        new PlaceSummaryReadModel(
                                1L,
                                "동국반점",
                                PlaceCategory.RESTAURANT,
                                PlaceSubCategory.CHINESE,
                                37.5585,
                                127.0,
                                null,
                                0,
                                List.of()),
                        new PlaceSummaryReadModel(
                                2L,
                                "필동커피",
                                PlaceCategory.CAFE,
                                PlaceSubCategory.COFFEE,
                                37.5586,
                                127.0,
                                null,
                                0,
                                List.of())));
        given(placeQueries.findRatingSummaries()).willReturn(Map.of(1L, new RatingSummary(4.3, 3)));
        given(placeQueries.findVisiblePlaceTags()).willReturn(Map.of(2L, List.of(PlaceTag.STUDY, PlaceTag.COFFEE)));

        List<PlaceSummaryReadModel> places = service.getPlaces();

        assertThat(places.get(0).averageRating()).isEqualTo(4.3);
        assertThat(places.get(0).reviewCount()).isEqualTo(3);
        assertThat(places.get(1).averageRating()).isNull();
        assertThat(places.get(1).reviewCount()).isZero();
        assertThat(places.get(0).tags()).isEmpty();
        assertThat(places.get(1).tags()).containsExactly(PlaceTag.STUDY, PlaceTag.COFFEE);
    }

    @Test
    void 없거나_숨긴_장소는_404다() {
        given(placeRepository.findByIdAndStatus(1L, PlaceStatus.VISIBLE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPlace(1L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND));
    }

    @Test
    void 중복_후보를_차단_대상과_안내_대상으로_나눈다() {
        given(placeRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                        eq(PlaceStatus.VISIBLE), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(
                        PlaceFixtures.place(1L, "동국반점", NEAR_CAMPUS),
                        PlaceFixtures.place(2L, "동국반점 2호점", NEAR_CAMPUS)));

        PlaceDuplicatesReadModel duplicates =
                service.findDuplicates("동국 반점", NEAR_CAMPUS.latitude(), NEAR_CAMPUS.longitude());

        assertThat(duplicates.exact())
                .extracting(PlaceDuplicatesReadModel.Candidate::id)
                .containsExactly(1L);
        assertThat(duplicates.similar())
                .extracting(PlaceDuplicatesReadModel.Candidate::id)
                .containsExactly(2L);
        assertThat(duplicates.exact().get(0).distanceMeters()).isZero();
    }

    @Test
    void 잘못된_좌표로_중복을_확인하면_400_3이다() {
        assertThatThrownBy(() -> service.findDuplicates("동국반점", 91, 0))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(PlaceErrorCode.INVALID_PLACE));
    }
}
