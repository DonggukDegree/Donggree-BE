package com.donggree.place.internal.infrastructure;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.domain.GeoPoint;
import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceEditHistory;
import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceInfo;
import com.donggree.place.internal.domain.PlaceMenu;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceTag;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, QuerydslPlaceQueries.class})
class PlacePersistenceTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceEditHistoryRepository editHistoryRepository;

    @Autowired
    private PlaceReportRepository reportRepository;

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

    @Test
    void 정보를_수정하면_메뉴를_교체하고_버전을_올리며_메뉴_전후_목록을_이력에_저장한다() {
        Long id = save("동국반점", NEAR_CAMPUS).getId();
        entityManager.clear();

        Place place = placeRepository.findById(id).orElseThrow();
        PlaceEditHistory history = place.editInfo(2L, new PlaceInfo(null, "후문 쪽", List.of(new MenuItem("짬뽕", 8000))))
                .orElseThrow();
        placeRepository.flush();
        editHistoryRepository.save(history);
        entityManager.flush();
        entityManager.clear();

        Place found = placeRepository.findById(id).orElseThrow();
        assertThat(found.getVersion()).isEqualTo(1L);
        assertThat(found.getBusinessHours()).isNull();
        assertThat(found.getMenus()).extracting(PlaceMenu::getName).containsExactly("짬뽕");
        assertThat(entityManager
                        .createNativeQuery("SELECT count(*) FROM place_menu WHERE place_id = :id")
                        .setParameter("id", id)
                        .getSingleResult())
                .isEqualTo(1L);

        PlaceEditHistory saved =
                editHistoryRepository.findByPlaceIdOrderByEditedAtAsc(id).getFirst();
        assertThat(saved.getEditorId()).isEqualTo(2L);
        assertThat(saved.before().menus()).containsExactly(new MenuItem("짜장면", 7000), new MenuItem("탕수육", null));
        assertThat(saved.after()).isEqualTo(found.info());
    }

    @Test
    void 같은_버전을_읽은_다른_수정이_먼저_저장되면_나중_저장은_충돌한다() {
        Long id = save("동국반점", NEAR_CAMPUS).getId();
        entityManager.clear();
        Place stale = placeRepository.findById(id).orElseThrow();
        entityManager
                .createNativeQuery("UPDATE place SET version = version + 1 WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();

        stale.editInfo(2L, new PlaceInfo("매일 10:00-22:00", null, List.of()));

        assertThatThrownBy(placeRepository::flush).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void 탈퇴_회원의_수정자_연결만_끊고_이력은_유지한다() {
        Place place = save("동국반점", NEAR_CAMPUS);
        PlaceEditHistory history =
                place.editInfo(2L, new PlaceInfo(null, null, List.of())).orElseThrow();
        placeRepository.flush();
        editHistoryRepository.save(history);
        entityManager.flush();

        assertThat(editHistoryRepository.clearEditor(2L)).isEqualTo(1);
        assertThat(editHistoryRepository.clearEditor(2L)).isZero();

        PlaceEditHistory found = editHistoryRepository.findById(history.getId()).orElseThrow();
        assertThat(found.getEditorId()).isNull();
        assertThat(found.getBeforeBusinessHours()).isEqualTo("평일 11:00-21:00");
    }

    @Test
    void 숨기면_그_장소의_처리_대기_신고만_처리_완료로_바꾸고_재신고를_허용한다() {
        Place target = save("동국반점", NEAR_CAMPUS);
        Place other = save("필동커피", NEAR_CAMPUS);
        PlaceReport pending =
                reportRepository.save(PlaceReport.submit(target.getId(), 7L, PlaceReportReason.CLOSED, null));
        PlaceReport otherPlace =
                reportRepository.save(PlaceReport.submit(other.getId(), 7L, PlaceReportReason.CLOSED, null));
        entityManager.flush();
        assertThat(reportRepository.existsByPlaceIdAndReporterIdAndStatus(
                        target.getId(), 7L, PlaceReportStatus.PENDING))
                .isTrue();

        LocalDateTime processedAt = LocalDateTime.of(2026, 11, 12, 18, 0);
        assertThat(reportRepository.resolvePending(target.getId(), processedAt)).isEqualTo(1);
        assertThat(reportRepository.resolvePending(target.getId(), processedAt)).isZero();

        PlaceReport resolved = reportRepository.findById(pending.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(PlaceReportStatus.RESOLVED);
        assertThat(resolved.getProcessedAt()).isEqualTo(processedAt);
        assertThat(reportRepository.findById(otherPlace.getId()).orElseThrow().isPending())
                .isTrue();
        assertThat(reportRepository.existsByPlaceIdAndReporterIdAndStatus(
                        target.getId(), 7L, PlaceReportStatus.PENDING))
                .isFalse();
    }

    @Test
    void 숨김_해제_시_처리_대기_신고만_기각하고_대기_신고_수를_다시_센다() {
        Place place = save("동국반점", NEAR_CAMPUS);
        reportRepository.save(PlaceReport.submit(place.getId(), 7L, PlaceReportReason.CLOSED, null));
        reportRepository.save(PlaceReport.submit(place.getId(), 8L, PlaceReportReason.WRONG_NAME, null));
        entityManager.flush();
        assertThat(reportRepository.countByPlaceIdAndStatus(place.getId(), PlaceReportStatus.PENDING))
                .isEqualTo(2);

        assertThat(reportRepository.rejectPending(place.getId(), LocalDateTime.of(2026, 11, 13, 9, 0)))
                .isEqualTo(2);

        assertThat(reportRepository.countByPlaceIdAndStatus(place.getId(), PlaceReportStatus.PENDING))
                .isZero();
        assertThat(reportRepository.findByPlaceIdOrderByIdAsc(place.getId())).allSatisfy(report -> {
            assertThat(report.getStatus()).isEqualTo(PlaceReportStatus.REJECTED);
            assertThat(report.getProcessedAt()).isNotNull();
        });
    }

    @Test
    void 탈퇴_회원의_신고자_연결만_끊고_신고는_유지한다() {
        Place place = save("동국반점", NEAR_CAMPUS);
        PlaceReport report =
                reportRepository.save(PlaceReport.submit(place.getId(), 7L, PlaceReportReason.OTHER, "주차장 입구"));
        entityManager.flush();

        assertThat(reportRepository.clearReporter(7L)).isEqualTo(1);
        assertThat(reportRepository.clearReporter(7L)).isZero();

        PlaceReport found = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(found.getReporterId()).isNull();
        assertThat(found.getDescription()).isEqualTo("주차장 입구");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 숨은_맛집_태그를_저장하고_지도_목록용으로_숨기지_않은_장소만_모은다() {
        Place visible = placeRepository.save(Place.register(new com.donggree.place.internal.domain.PlaceRegistration(
                1L,
                "동국반점",
                com.donggree.place.internal.domain.enums.PlaceCategory.RESTAURANT,
                com.donggree.place.internal.domain.enums.PlaceSubCategory.CHINESE,
                NEAR_CAMPUS,
                null,
                null,
                List.of(),
                List.of(PlaceTag.SOLO_DINING, PlaceTag.COFFEE))));
        Place hidden = placeRepository.save(Place.register(new com.donggree.place.internal.domain.PlaceRegistration(
                1L,
                "숨김식당",
                com.donggree.place.internal.domain.enums.PlaceCategory.RESTAURANT,
                com.donggree.place.internal.domain.enums.PlaceSubCategory.CHINESE,
                NEAR_CAMPUS,
                null,
                null,
                List.of(),
                List.of(PlaceTag.MOOD))));
        save("태그없음", NEAR_CAMPUS);
        hidden.hide();
        entityManager.flush();
        entityManager.clear();

        assertThat(placeRepository
                        .findById(visible.getId())
                        .orElseThrow()
                        .info()
                        .tags())
                .containsExactly(PlaceTag.COFFEE, PlaceTag.SOLO_DINING);
        assertThat(placeQueries.findVisiblePlaceTags())
                .containsOnlyKeys(visible.getId())
                .containsEntry(visible.getId(), List.of(PlaceTag.COFFEE, PlaceTag.SOLO_DINING));
    }

    @Test
    void 태그를_수정하면_버전이_오르고_이력에_태그_전후가_남는다() {
        Long id = save("동국반점", NEAR_CAMPUS).getId();
        entityManager.clear();
        Place place = placeRepository.findById(id).orElseThrow();
        PlaceInfo base = place.info();

        PlaceEditHistory history = place.editInfo(
                        2L,
                        new PlaceInfo(base.businessHours(), base.description(), base.menus(), List.of(PlaceTag.STUDY)))
                .orElseThrow();
        placeRepository.flush();
        editHistoryRepository.save(history);
        entityManager.flush();
        entityManager.clear();

        assertThat(placeRepository.findById(id).orElseThrow().getVersion()).isEqualTo(1L);
        PlaceEditHistory saved =
                editHistoryRepository.findByPlaceIdOrderByEditedAtAsc(id).getFirst();
        assertThat(saved.before().tags()).isEmpty();
        assertThat(saved.after().tags()).containsExactly(PlaceTag.STUDY);
    }

    private Place save(String name, GeoPoint point) {
        Place place = placeRepository.save(Place.register(PlaceFixtures.registration(name, point)));
        entityManager.flush();
        return place;
    }
}
