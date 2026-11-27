package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.club.internal.application.ClubImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClubImageStorageSelectionTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(DiskClubImageStorage.class, R2ClubImageStorage.class);

    @Test
    void 설정이_없으면_디스크를_쓴다() {
        runner.run(context ->
                assertThat(context.getBean(ClubImageStorage.class)).isInstanceOf(DiskClubImageStorage.class));
    }

    @Test
    void r2를_고르면_R2를_쓰고_필수_설정이_비면_시작하지_않는다() {
        runner.withPropertyValues(
                        "club.image.storage=r2",
                        "club.image.r2.endpoint=https://account.r2.cloudflarestorage.com",
                        "club.image.r2.bucket=donggree-club-images",
                        "club.image.r2.access-key-id=id",
                        "club.image.r2.secret-access-key=secret")
                .run(context ->
                        assertThat(context.getBean(ClubImageStorage.class)).isInstanceOf(R2ClubImageStorage.class));
        runner.withPropertyValues("club.image.storage=r2")
                .run(context -> assertThat(context).hasFailed());
    }
}
