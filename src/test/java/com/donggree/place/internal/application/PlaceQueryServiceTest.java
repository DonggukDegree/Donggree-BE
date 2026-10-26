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
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlaceQueryServiceTest {

    private final PlaceQueries placeQueries = mock(PlaceQueries.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceQueryService service =
            new PlaceQueryService(placeQueries, placeRepository, new PlaceDuplicateFinder(placeRepository));

    @Test
    void 노출_장소의_상세와_메뉴를_반환한다() {
        given(placeRepository.findByIdAndStatus(1L, PlaceStatus.VISIBLE))
                .willReturn(Optional.of(PlaceFixtures.place(1L, "동국반점", NEAR_CAMPUS)));

        PlaceDetailReadModel detail = service.getPlace(1L);

        assertThat(detail.name()).isEqualTo("동국반점");
        assertThat(detail.menus())
                .containsExactly(
                        new PlaceDetailReadModel.Menu("짜장면", 7000), new PlaceDetailReadModel.Menu("탕수육", null));
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
