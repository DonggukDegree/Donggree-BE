package com.donggree.place.internal.domain;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceDuplicatesTest {

    private static final GeoPoint THIRTY_METERS_NORTH =
            new GeoPoint(NEAR_CAMPUS.latitude() + 0.00027, NEAR_CAMPUS.longitude());
    private static final GeoPoint HUNDRED_METERS_NORTH =
            new GeoPoint(NEAR_CAMPUS.latitude() + 0.0009, NEAR_CAMPUS.longitude());

    @Test
    void 반경_50m_안의_정규화_이름이_같으면_차단_대상이다() {
        Place existing = PlaceFixtures.place(1L, "Cafe 동국", THIRTY_METERS_NORTH);

        PlaceDuplicates duplicates = PlaceDuplicates.of("cafe동국!", NEAR_CAMPUS, List.of(existing));

        assertThat(duplicates.hasExact()).isTrue();
        assertThat(duplicates.exact()).extracting(m -> m.place().getId()).containsExactly(1L);
        assertThat(duplicates.exact().get(0).distanceMeters()).isBetween(25.0, 35.0);
        assertThat(duplicates.similar()).isEmpty();
    }

    @Test
    void 한쪽_이름이_다른_쪽을_포함하면_안내_대상이다() {
        Place branch = PlaceFixtures.place(1L, "동국반점 2호점", THIRTY_METERS_NORTH);
        Place shorter = PlaceFixtures.place(2L, "동국", NEAR_CAMPUS);

        PlaceDuplicates duplicates = PlaceDuplicates.of("동국반점", NEAR_CAMPUS, List.of(branch, shorter));

        assertThat(duplicates.hasExact()).isFalse();
        assertThat(duplicates.similar()).extracting(m -> m.place().getId()).containsExactly(2L, 1L);
    }

    @Test
    void 반경_밖이거나_이름이_다르면_후보가_아니다() {
        Place far = PlaceFixtures.place(1L, "동국반점", HUNDRED_METERS_NORTH);
        Place other = PlaceFixtures.place(2L, "충무김밥", NEAR_CAMPUS);

        PlaceDuplicates duplicates = PlaceDuplicates.of("동국반점", NEAR_CAMPUS, List.of(far, other));

        assertThat(duplicates.exact()).isEmpty();
        assertThat(duplicates.similar()).isEmpty();
    }

    @Test
    void 정규화_이름이_비면_비교하지_않는다() {
        Place existing = PlaceFixtures.place(1L, "동국반점", NEAR_CAMPUS);

        PlaceDuplicates duplicates = PlaceDuplicates.of("★", NEAR_CAMPUS, List.of(existing));

        assertThat(duplicates.exact()).isEmpty();
        assertThat(duplicates.similar()).isEmpty();
    }
}
