package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubFavorite;
import com.donggree.club.internal.domain.ClubFavoriteRepository;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class ClubFavoritePersistenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubFavoriteRepository favoriteRepository;

    @Test
    void 최근_찜한_순으로_동아리_ID를_주고_탈퇴하면_지운다() {
        Long band = save("소리마루");
        Long book = save("책갈피");
        favoriteRepository.saveAndFlush(ClubFavorite.of(7L, band, NOW));
        favoriteRepository.saveAndFlush(ClubFavorite.of(7L, book, NOW.plusMinutes(1)));
        favoriteRepository.saveAndFlush(ClubFavorite.of(8L, band, NOW));

        assertThat(favoriteRepository.findClubIds(7L)).containsExactly(book, band);
        assertThat(favoriteRepository.deleteByMember(7L)).isEqualTo(2);
        assertThat(favoriteRepository.findClubIds(7L)).isEmpty();
        assertThat(favoriteRepository.findClubIds(8L)).containsExactly(band);
    }

    private Long save(String name) {
        return clubRepository
                .saveAndFlush(Club.register(ClubFixtures.profile(name), ClubRegistration.OFFICIAL, NOW))
                .getId();
    }
}
