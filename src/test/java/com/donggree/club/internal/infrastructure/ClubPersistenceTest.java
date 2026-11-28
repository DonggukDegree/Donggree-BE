package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.application.ClubImageUrls;
import com.donggree.club.internal.application.readmodel.ClubListRow;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubLink;
import com.donggree.club.internal.domain.ClubOfficer;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.enums.ClubField;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, QuerydslClubQueries.class, ClubImageUrls.class})
class ClubPersistenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubRecruitmentRepository recruitmentRepository;

    @Autowired
    private QuerydslClubQueries clubQueries;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 운영진과_링크를_jsonb로_입력_순서대로_저장한다() {
        Long id = save("소리마루").getId();
        entityManager.clear();

        Club found = clubRepository.findById(id).orElseThrow();

        assertThat(found.getOfficers()).containsExactly(new ClubOfficer("회장", "컴퓨터공학과", "김동글"));
        assertThat(found.getLinks()).containsExactly(new ClubLink("인스타그램", "https://instagram.com/donggree"));
        assertThat(found.sortedFields()).containsExactly(ClubField.PERFORMANCE);
        assertThat(found.getVersion()).isZero();
    }

    @Test
    void 목록_값은_동아리별_가장_최근_모집_건과_분야를_담는다() {
        Club band = save("소리마루");
        Club book = save("책갈피");
        recruitmentRepository.save(
                ClubRecruitment.open(band.getId(), ClubFixtures.period(NOW.minusDays(40), NOW.minusDays(30))));
        ClubRecruitment latest =
                recruitmentRepository.save(ClubRecruitment.open(band.getId(), ClubFixtures.alwaysOpen()));
        entityManager.flush();
        entityManager.clear();

        List<ClubListRow> rows = clubQueries.findListRows();

        assertThat(rows).extracting(ClubListRow::name).containsExactly("소리마루", "책갈피");
        assertThat(rows.get(0).latestRecruitment().id()).isEqualTo(latest.getId());
        assertThat(rows.get(0).latestRecruitment().alwaysOpen()).isTrue();
        assertThat(rows.get(0).fields()).containsExactly(ClubField.PERFORMANCE);
        assertThat(rows.get(1).latestRecruitment()).isNull();
        assertThat(recruitmentRepository.findTopByClubIdOrderByIdDesc(band.getId()))
                .get()
                .extracting(ClubRecruitment::getId)
                .isEqualTo(latest.getId());
        assertThat(recruitmentRepository.findTopByClubIdOrderByIdDesc(book.getId()))
                .isEmpty();
    }

    @Test
    void 기수_없이_활동_기간만_두면_DB가_거절한다() {
        Long id = save("소리마루").getId();

        assertThatThrownBy(() -> entityManager
                        .createNativeQuery("UPDATE club SET generation = NULL WHERE id = :id")
                        .setParameter("id", id)
                        .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    private Club save(String name) {
        Club club = clubRepository.save(Club.register(ClubFixtures.profile(name), ClubRegistration.OFFICIAL, NOW));
        entityManager.flush();
        return club;
    }
}
