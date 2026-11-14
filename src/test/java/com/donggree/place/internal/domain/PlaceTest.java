package com.donggree.place.internal.domain;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    @Test
    void 정보를_수정하면_메뉴를_전체_교체하고_변경_전후_이력을_만든다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        LocalDateTime registeredAt = place.getInfoUpdatedAt();

        PlaceEditHistory history = place.editInfo(
                        2L,
                        new PlaceInfo(
                                " 매일 10:00-22:00 ",
                                null,
                                List.of(new MenuItem(" 짬뽕 ", 8000), new MenuItem("짜장면", 7500))))
                .orElseThrow();

        assertThat(place.getBusinessHours()).isEqualTo("매일 10:00-22:00");
        assertThat(place.getDescription()).isNull();
        assertThat(place.getMenus())
                .extracting(PlaceMenu::getName, PlaceMenu::getPrice, PlaceMenu::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("짬뽕", 8000, 0),
                        org.assertj.core.groups.Tuple.tuple("짜장면", 7500, 1));
        assertThat(place.getInfoUpdatedAt()).isAfterOrEqualTo(registeredAt);
        assertThat(place.getName()).isEqualTo("동국반점");
        assertThat(place.getCreatedBy()).isEqualTo(1L);

        assertThat(history.getPlaceId()).isEqualTo(10L);
        assertThat(history.getEditorId()).isEqualTo(2L);
        assertThat(history.getEditedAt()).isEqualTo(place.getInfoUpdatedAt());
        assertThat(history.before())
                .isEqualTo(new PlaceInfo(
                        "평일 11:00-21:00", "정문 맞은편", List.of(new MenuItem("짜장면", 7000), new MenuItem("탕수육", null))));
        assertThat(history.after()).isEqualTo(place.info());
    }

    @Test
    void 정규화한_값이_그대로면_수정하지_않고_이력도_만들지_않는다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        LocalDateTime registeredAt = place.getInfoUpdatedAt();

        assertThat(place.editInfo(
                        2L,
                        new PlaceInfo(
                                "  평일 11:00-21:00",
                                "정문 맞은편\n",
                                List.of(new MenuItem("짜장면 ", 7000), new MenuItem("탕수육", null)))))
                .isEmpty();
        assertThat(place.getInfoUpdatedAt()).isEqualTo(registeredAt);
    }

    @Test
    void 메뉴_순서만_바꿔도_수정으로_본다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);

        assertThat(place.editInfo(
                        2L,
                        new PlaceInfo(
                                "평일 11:00-21:00",
                                "정문 맞은편",
                                List.of(new MenuItem("탕수육", null), new MenuItem("짜장면", 7000)))))
                .isPresent();
    }

    @Test
    void 수정값도_등록과_같은_제약을_검사하고_실패하면_기존_값을_유지한다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);

        assertThatThrownBy(() -> place.editInfo(2L, new PlaceInfo("가".repeat(101), null, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> place.editInfo(2L, new PlaceInfo(null, "가".repeat(301), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> place.editInfo(
                        2L, new PlaceInfo(null, null, Collections.nCopies(51, new MenuItem("메뉴", 1000)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> place.editInfo(
                        2L, new PlaceInfo(null, null, List.of(new MenuItem("메뉴", 1000), new MenuItem("", 1000)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> place.editInfo(2L, new PlaceInfo(null, null, Arrays.asList((MenuItem) null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> place.editInfo(null, new PlaceInfo(null, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(place.getBusinessHours()).isEqualTo("평일 11:00-21:00");
        assertThat(place.getMenus()).hasSize(2);
    }

    @Test
    void 관리자_숨김과_해제는_노출_상태만_바꾼다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        LocalDateTime infoUpdatedAt = place.getInfoUpdatedAt();

        place.hide();
        assertThat(place.isVisible()).isFalse();
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.HIDDEN);
        place.hide();
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.HIDDEN);
        place.show();
        assertThat(place.isVisible()).isTrue();
        assertThat(place.getInfoUpdatedAt()).isEqualTo(infoUpdatedAt);
    }

    @Test
    void 숨은_맛집_태그는_중복을_없애고_enum_순서로_저장한다() {
        Place place = Place.register(new PlaceRegistration(
                1L,
                "동국반점",
                PlaceCategory.RESTAURANT,
                PlaceSubCategory.CHINESE,
                NEAR_CAMPUS,
                null,
                null,
                null,
                List.of(PlaceTag.SOLO_DINING, PlaceTag.COFFEE, PlaceTag.SOLO_DINING)));

        assertThat(place.info().tags()).containsExactly(PlaceTag.SOLO_DINING, PlaceTag.COFFEE);
        assertThat(place.getTags()).hasSize(2);
    }

    @Test
    void 태그도_공동_편집_대상이며_순서만_다르면_바뀐_것으로_보지_않는다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        PlaceInfo base = place.info();

        PlaceEditHistory history = place.editInfo(
                        2L,
                        new PlaceInfo(
                                base.businessHours(),
                                base.description(),
                                base.menus(),
                                List.of(PlaceTag.MEAL_DATE, PlaceTag.MOOD)))
                .orElseThrow();
        assertThat(history.before().tags()).isEmpty();
        assertThat(history.after().tags()).containsExactly(PlaceTag.MEAL_DATE, PlaceTag.MOOD);

        assertThat(place.editInfo(
                        2L,
                        new PlaceInfo(
                                base.businessHours(),
                                base.description(),
                                base.menus(),
                                List.of(PlaceTag.MOOD, PlaceTag.MEAL_DATE, PlaceTag.MOOD))))
                .isEmpty();
        assertThatThrownBy(() -> place.editInfo(
                        2L, new PlaceInfo(null, null, List.of(), java.util.Arrays.asList((PlaceTag) null))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 개편된_상세카테고리는_각_카테고리에만_속한다() {
        assertThat(PlaceSubCategory.FRANCHISE.belongsTo(PlaceCategory.CAFE)).isTrue();
        assertThat(PlaceSubCategory.CAFETERIA.belongsTo(PlaceCategory.RESTAURANT))
                .isTrue();
        assertThat(PlaceSubCategory.CAFETERIA.getLabel()).isEqualTo("학식당");
        assertThat(PlaceSubCategory.LIBRARY.belongsTo(PlaceCategory.STUDY)).isTrue();
        assertThat(PlaceSubCategory.FITNESS.belongsTo(PlaceCategory.LIFE)).isTrue();
        assertThat(PlaceSubCategory.PC_ROOM.belongsTo(PlaceCategory.ENTERTAINMENT))
                .isTrue();
        assertThat(java.util.Arrays.stream(PlaceSubCategory.values()).map(Enum::name))
                .doesNotContain("POCHA", "STUDY_FRIENDLY_CAFE");
        for (PlaceCategory category : PlaceCategory.values()) {
            assertThat(java.util.Arrays.stream(PlaceSubCategory.values()).filter(sub -> sub.belongsTo(category)))
                    .as(category.name())
                    .isNotEmpty();
        }
    }

    private static PlaceRegistration menus(List<MenuItem> menus) {
        return new PlaceRegistration(
                1L, "동국반점", PlaceCategory.RESTAURANT, PlaceSubCategory.CHINESE, NEAR_CAMPUS, null, null, menus);
    }
}
