package com.donggree.place.internal.infrastructure;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.domain.GeoPoint;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceMenu;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, QuerydslPlaceQueries.class})
class PlacePersistenceTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private QuerydslPlaceQueries placeQueries;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 메뉴를_입력_순서대로_저장하고_조회한다() {
        Long id = save("동국반점", NEAR_CAMPUS).getId();
        entityManager.clear();

        Place found = placeRepository.findByIdAndStatus(id, PlaceStatus.VISIBLE).orElseThrow();

        assertThat(found.getMenus()).extracting(PlaceMenu::getName).containsExactly("짜장면", "탕수육");
        assertThat(found.getVersion()).isZero();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 지도_목록은_숨김_장소를_제외하고_핀_정보만_반환한다() {
        Place visible = save("동국반점", NEAR_CAMPUS);
        Place hidden = save("숨김식당", NEAR_CAMPUS);
        entityManager
                .createNativeQuery("UPDATE place SET status = 'HIDDEN' WHERE id = :id")
                .setParameter("id", hidden.getId())
                .executeUpdate();

        assertThat(placeQueries.findVisibleSummaries())
                .extracting(PlaceSummaryReadModel::id)
                .containsExactly(visible.getId());
        assertThat(placeRepository.findByIdAndStatus(hidden.getId(), PlaceStatus.VISIBLE))
                .isEmpty();
    }

    @Test
    void 사각_범위_안의_노출_장소만_중복_후보로_조회한다() {
        Place inside = save("동국반점", NEAR_CAMPUS);
        save("먼식당", new GeoPoint(NEAR_CAMPUS.latitude() + 0.002, NEAR_CAMPUS.longitude()));
        GeoPoint.BoundingBox box = NEAR_CAMPUS.boundingBox(50);

        assertThat(placeRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                        PlaceStatus.VISIBLE,
                        box.minLatitude(),
                        box.maxLatitude(),
                        box.minLongitude(),
                        box.maxLongitude()))
                .extracting(Place::getId)
                .containsExactly(inside.getId());
    }

    @Test
    void 탈퇴_회원의_등록자_연결만_끊고_수정_정보는_유지한다() {
        Place place = save("동국반점", NEAR_CAMPUS);
        entityManager.clear();
        var infoUpdatedAt =
                placeRepository.findById(place.getId()).orElseThrow().getInfoUpdatedAt();

        assertThat(placeRepository.clearCreatedBy(1L)).isEqualTo(1);
        assertThat(placeRepository.clearCreatedBy(1L)).isZero();

        Place found = placeRepository.findById(place.getId()).orElseThrow();
        assertThat(found.getCreatedBy()).isNull();
        assertThat(found.getVersion()).isZero();
        assertThat(found.getInfoUpdatedAt()).isEqualTo(infoUpdatedAt);
    }

    private Place save(String name, GeoPoint point) {
        Place place = placeRepository.save(Place.register(PlaceFixtures.registration(name, point)));
        entityManager.flush();
        return place;
    }
}
