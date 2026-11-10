package com.donggree.place.internal.domain;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceTest {

    @Test
    void 장소를_등록하면_노출_상태로_메뉴를_입력_순서대로_저장한다() {
        Place place = Place.register(PlaceFixtures.registration("  동국반점  ", NEAR_CAMPUS));

        assertThat(place.getName()).isEqualTo("동국반점");
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.VISIBLE);
        assertThat(place.getCreatedBy()).isEqualTo(1L);
        assertThat(place.getInfoUpdatedAt()).isNotNull();
        assertThat(place.point()).isEqualTo(NEAR_CAMPUS);
        assertThat(place.getMenus())
                .extracting(PlaceMenu::getName, PlaceMenu::getPrice, PlaceMenu::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("짜장면", 7000, 0),
                        org.assertj.core.groups.Tuple.tuple("탕수육", null, 1));
    }

    @Test
    void 공백만_입력한_선택_항목은_미입력으로_저장한다() {
        Place place = Place.register(new PlaceRegistration(
                1L, "동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, NEAR_CAMPUS, "  ", "\n", null));

        assertThat(place.getBusinessHours()).isNull();
        assertThat(place.getDescription()).isNull();
        assertThat(place.getMenus()).isEmpty();
    }

    @Test
    void 이름은_1자에서_30자까지만_허용한다() {
        assertThatThrownBy(() -> Place.register(PlaceFixtures.registration(" ", NEAR_CAMPUS)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Place.register(PlaceFixtures.registration("가".repeat(31), NEAR_CAMPUS)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상세카테고리가_카테고리에_속하지_않으면_등록할_수_없다() {
        assertThatThrownBy(() -> Place.register(new PlaceRegistration(
                        1L, "동국반점", PlaceCategory.CAFE, PlaceSubCategory.CHINESE, NEAR_CAMPUS, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 서비스_영역_밖에는_등록할_수_없다() {
        assertThatThrownBy(() -> Place.register(PlaceFixtures.registration("동국반점", new GeoPoint(37.6, 127.0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 영업시간과_설명의_최대_길이를_검사한다() {
        assertThatThrownBy(() -> Place.register(new PlaceRegistration(
                        1L,
                        "동국반점",
                        PlaceCategory.RESTAURANT,
                        PlaceSubCategory.CHINESE,
                        NEAR_CAMPUS,
                        "가".repeat(101),
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Place.register(new PlaceRegistration(
                        1L,
                        "동국반점",
                        PlaceCategory.RESTAURANT,
                        PlaceSubCategory.CHINESE,
                        NEAR_CAMPUS,
                        null,
                        "가".repeat(301),
                        null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 메뉴는_50개까지_가격은_0원에서_100만원까지_허용한다() {
        List<MenuItem> tooMany = Collections.nCopies(51, new MenuItem("메뉴", 1000));
        assertThatThrownBy(() -> Place.register(menus(tooMany))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Place.register(menus(List.of(new MenuItem("메뉴", -1)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Place.register(menus(List.of(new MenuItem("메뉴", 1_000_001)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Place.register(menus(List.of(new MenuItem(" ", 1000)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Place.register(menus(Collections.nCopies(50, new MenuItem("메뉴", 1_000_000))))
                        .getMenus())
                .hasSize(50);
    }

    @Test
    void 등록자_없이는_등록할_수_없다() {
        assertThatThrownBy(() -> Place.register(new PlaceRegistration(
                        null,
                        "동국반점",
                        PlaceCategory.RESTAURANT,
                        PlaceSubCategory.CHINESE,
                        NEAR_CAMPUS,
                        null,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static PlaceRegistration menus(List<MenuItem> menus) {
        return new PlaceRegistration(
                1L, "동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, NEAR_CAMPUS, null, null, menus);
    }
}
