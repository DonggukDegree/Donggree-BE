package com.donggree.club.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.application.readmodel.ClubListRow;
import com.donggree.club.internal.application.readmodel.ClubPageReadModel;
import com.donggree.club.internal.application.readmodel.ClubRecruitmentReadModel;
import com.donggree.club.internal.application.readmodel.ClubSummaryReadModel;
import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ClubCatalogTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);

    private static final ClubListRow BAND = row(
            1L,
            "소리마루",
            ClubAffiliation.CENTRAL,
            ClubField.PERFORMANCE,
            120,
            LocalDate.of(1998, 3, 2),
            period(NOW.minusDays(3), NOW.plusDays(5)));
    private static final ClubListRow CODE = row(
            2L,
            "코드나무",
            ClubAffiliation.DEPARTMENT,
            ClubField.SCIENCE_IT,
            40,
            null,
            period(NOW.minusDays(1), NOW.plusDays(2)));
    private static final ClubListRow VOLUNTEER =
            row(3L, "가온누리", ClubAffiliation.UNION, ClubField.VOLUNTEER, null, LocalDate.of(2010, 5, 1), always());
    private static final ClubListRow BOOK = row(
            4L,
            "책갈피",
            ClubAffiliation.CENTRAL,
            ClubField.ACADEMIC,
            300,
            LocalDate.of(1990, 1, 1),
            period(NOW.plusDays(3), NOW.plusDays(10)));
    private static final ClubListRow GO =
            row(5L, "한수바둑", ClubAffiliation.CENTRAL, ClubField.CULTURE_HOBBY, 15, null, null);
    private static final List<ClubListRow> ROWS = List.of(BAND, CODE, VOLUNTEER, BOOK, GO);

    @Test
    void 이름순은_한글_가나다순이다() {
        assertThat(names(page(search(ClubSort.NAME), null))).containsExactly("가온누리", "소리마루", "책갈피", "코드나무", "한수바둑");
    }

    @Test
    void 마감_임박순은_기간_모집_마감순_다음_상시모집_다음_나머지다() {
        assertThat(names(page(search(ClubSort.DEADLINE), null))).containsExactly("코드나무", "소리마루", "가온누리", "책갈피", "한수바둑");
    }

    @Test
    void 인원순과_오래된순은_값이_없는_동아리를_맨_뒤에_둔다() {
        assertThat(names(page(search(ClubSort.MEMBERS), null))).containsExactly("책갈피", "소리마루", "코드나무", "한수바둑", "가온누리");
        assertThat(names(page(search(ClubSort.OLDEST), null))).containsExactly("책갈피", "소리마루", "가온누리", "코드나무", "한수바둑");
    }

    @Test
    void 필터_종류끼리는_모두_같은_필터_안에서는_하나라도_맞으면_통과한다() {
        ClubSearch openCentral = new ClubSearch(
                Set.of(ClubAffiliation.CENTRAL), Set.of(), Set.of(RecruitmentStatus.OPEN), ClubSort.NAME, null);
        ClubSearch beforeOrIt = new ClubSearch(
                Set.of(),
                Set.of(ClubField.SCIENCE_IT, ClubField.ACADEMIC, ClubField.CULTURE_HOBBY),
                Set.of(RecruitmentStatus.BEFORE),
                ClubSort.NAME,
                null);
        ClubSearch favorites = new ClubSearch(Set.of(), Set.of(), Set.of(), ClubSort.NAME, Set.of(2L, 5L, 99L));

        assertThat(names(page(openCentral, null))).containsExactly("소리마루");
        assertThat(names(page(beforeOrIt, null))).containsExactly("책갈피", "한수바둑");
        ClubPageReadModel favoritePage = page(favorites, null);
        assertThat(names(favoritePage)).containsExactly("코드나무", "한수바둑");
        assertThat(favoritePage.totalCount()).isEqualTo(2);
    }

    @Test
    void 열두_개씩_커서로_이어_받는다() {
        List<ClubListRow> many = new ArrayList<>();
        IntStream.rangeClosed(1, 25)
                .forEach(id -> many.add(row(
                        id,
                        "동아리" + String.format("%02d", id),
                        ClubAffiliation.CENTRAL,
                        ClubField.ACADEMIC,
                        null,
                        null,
                        null)));

        ClubPageReadModel first = ClubCatalog.page(many, search(ClubSort.NAME), null, NOW);
        ClubPageReadModel second =
                ClubCatalog.page(many, search(ClubSort.NAME), ClubCursor.decode(first.nextCursor()), NOW);
        ClubPageReadModel last =
                ClubCatalog.page(many, search(ClubSort.NAME), ClubCursor.decode(second.nextCursor()), NOW);

        assertThat(first.clubs()).hasSize(12);
        assertThat(first.totalCount()).isEqualTo(25);
        assertThat(second.clubs().get(0).name()).isEqualTo("동아리13");
        assertThat(names(last)).containsExactly("동아리25");
        assertThat(last.nextCursor()).isNull();
    }

    @Test
    void 커서_이후_동아리가_추가되거나_빠져도_중복_없이_이어진다() {
        List<ClubListRow> many = new ArrayList<>();
        IntStream.rangeClosed(1, 14)
                .forEach(id -> many.add(row(
                        id,
                        "동아리" + String.format("%02d", id),
                        ClubAffiliation.CENTRAL,
                        ClubField.ACADEMIC,
                        null,
                        null,
                        null)));
        ClubPageReadModel first = ClubCatalog.page(many, search(ClubSort.NAME), null, NOW);
        many.remove(0);
        many.add(row(99, "가나다", ClubAffiliation.CENTRAL, ClubField.ACADEMIC, null, null, null));

        ClubPageReadModel next =
                ClubCatalog.page(many, search(ClubSort.NAME), ClubCursor.decode(first.nextCursor()), NOW);

        assertThat(names(next)).containsExactly("동아리13", "동아리14");
    }

    @Test
    void 목록_카드는_현재_모집과_활동_확인을_계산한다() {
        ClubSummaryReadModel band = page(search(ClubSort.NAME), null).clubs().get(1);

        assertThat(band.recruitment().id()).isEqualTo(1L);
        assertThat(band.activityConfirmed()).isTrue();
        assertThat(band.imageUrls()).isEmpty();
    }

    @Test
    void 형식이_맞지_않는_커서는_거절한다() {
        assertThatThrownBy(() -> ClubCursor.decode("not-a-cursor")).isInstanceOf(IllegalArgumentException.class);
    }

    private static ClubPageReadModel page(ClubSearch search, ClubCursor cursor) {
        return ClubCatalog.page(ROWS, search, cursor, NOW);
    }

    private static ClubSearch search(ClubSort sort) {
        return new ClubSearch(null, null, null, sort, null);
    }

    private static List<String> names(ClubPageReadModel page) {
        return page.clubs().stream().map(ClubSummaryReadModel::name).toList();
    }

    private static ClubRecruitmentReadModel period(LocalDateTime startsAt, LocalDateTime endsAt) {
        return new ClubRecruitmentReadModel(1L, 25, false, startsAt, endsAt, false, List.of(), null);
    }

    private static ClubRecruitmentReadModel always() {
        return new ClubRecruitmentReadModel(2L, null, true, null, null, false, List.of(), null);
    }

    private static ClubListRow row(
            long id,
            String name,
            ClubAffiliation affiliation,
            ClubField field,
            Integer totalMembers,
            LocalDate foundedDate,
            ClubRecruitmentReadModel recruitment) {
        return new ClubListRow(
                id,
                name,
                affiliation,
                List.of(field),
                ClubRegistration.OFFICIAL,
                24,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 2, 28),
                NOW.minusMonths(1),
                totalMembers,
                foundedDate,
                recruitment);
    }
}
