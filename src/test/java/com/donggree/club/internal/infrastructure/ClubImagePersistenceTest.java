package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.club.internal.application.ClubImageUrls;
import com.donggree.club.internal.application.ClubPhotoBinder;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubFixtures;
import com.donggree.club.internal.domain.ClubImage;
import com.donggree.club.internal.domain.ClubImageRepository;
import com.donggree.club.internal.domain.ClubPhotos;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.ImageFormat;
import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
    JpaAuditingConfig.class,
    QueryDslConfig.class,
    QuerydslClubQueries.class,
    ClubPhotoBinder.class,
    ClubImageUrls.class
})
class ClubImagePersistenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);

    @Autowired
    private ClubImageRepository imageRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubRequestRepository requestRepository;

    @Autowired
    private ClubPhotoBinder photoBinder;

    @Autowired
    private QuerydslClubQueries clubQueries;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 선택대로_연결하고_빠진_사진은_끊으며_목록은_대표_사진을_먼저_준다() {
        Club club = clubRepository.saveAndFlush(
                Club.register(ClubFixtures.profile("소리마루"), ClubRegistration.OFFICIAL, NOW));
        ClubImage first = upload(NOW, ClubImagePurpose.CLUB);
        ClubImage second = upload(NOW, ClubImagePurpose.CLUB);
        ClubImage third = upload(NOW, ClubImagePurpose.CLUB);

        photoBinder.bind(club.getId(), new ClubPhotos(List.of(first.getId(), second.getId()), second.getId()));
        entityManager.flush();
        assertThat(clubQueries.findListRows().get(0).imageUrls())
                .containsExactly(
                        ClubImageUrls.API_PATH + second.getStoredName(),
                        ClubImageUrls.API_PATH + first.getStoredName());

        photoBinder.bind(club.getId(), new ClubPhotos(List.of(third.getId(), second.getId()), null));
        entityManager.flush();
        entityManager.clear();

        assertThat(photoBinder.current(club.getId()))
                .isEqualTo(new ClubPhotos(List.of(third.getId(), second.getId()), third.getId()));
        assertThat(imageRepository.findById(first.getId()).orElseThrow().getClubId())
                .isNull();
    }

    @Test
    void 오래된_연결_안_된_동아리_사진만_정리_대상이고_검토_중_요청_사진과_배너_사진은_남긴다() {
        ClubImage old = upload(NOW.minusDays(31), ClubImagePurpose.CLUB);
        ClubImage pending = upload(NOW.minusDays(31), ClubImagePurpose.CLUB);
        ClubImage recent = upload(NOW.minusDays(1), ClubImagePurpose.CLUB);
        ClubImage banner = upload(NOW.minusDays(90), ClubImagePurpose.BANNER);
        requestRepository.saveAndFlush(ClubRequest.register(
                7L,
                new ClubRequester(ClubRequesterRole.STAFF, "dongle@dgu.ac.kr"),
                ClubRequestPayload.of(
                        ClubFixtures.profile("별자리 탐험대"), new ClubPhotos(List.of(pending.getId()), null))));

        assertThat(imageRepository.findExpiredUnattached(NOW.minusDays(30)))
                .extracting(ClubImage::getId)
                .containsExactly(old.getId())
                .doesNotContain(pending.getId(), recent.getId(), banner.getId());
    }

    private ClubImage upload(LocalDateTime at, ClubImagePurpose purpose) {
        return imageRepository.saveAndFlush(ClubImage.upload(ImageFormat.WEBP, 100, purpose, 7L, at));
    }
}
