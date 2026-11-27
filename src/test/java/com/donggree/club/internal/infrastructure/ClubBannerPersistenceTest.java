package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.club.internal.domain.BannerContent;
import com.donggree.club.internal.domain.ClubBanner;
import com.donggree.club.internal.domain.ClubBannerRepository;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class ClubBannerPersistenceTest {

    @Autowired
    private ClubBannerRepository bannerRepository;

    @Test
    void 노출_순서대로_조회하고_마지막_순서를_안다() {
        assertThat(bannerRepository.findMaxSortOrder()).isZero();
        bannerRepository.saveAndFlush(ClubBanner.create(text("두 번째"), 2));
        bannerRepository.saveAndFlush(ClubBanner.create(
                new BannerContent("광고", null, null, null, "/api/clubs/images/a.webp", null, "/clubs", null, null, true),
                1));

        assertThat(bannerRepository.findAllByOrderBySortOrderAscIdAsc())
                .extracting(ClubBanner::getTitle)
                .containsExactly("광고", "두 번째");
        assertThat(bannerRepository.findMaxSortOrder()).isEqualTo(2);
    }

    private static BannerContent text(String title) {
        return new BannerContent(title, "EVENT", null, "#2563eb", null, null, "/clubs", null, null, true);
    }
}
