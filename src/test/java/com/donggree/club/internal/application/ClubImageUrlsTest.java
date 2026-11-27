package com.donggree.club.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.club.internal.domain.ClubImage;
import com.donggree.club.internal.domain.ImageFormat;
import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ClubImageUrlsTest {

    private static final ClubImage IMAGE =
            ClubImage.upload(ImageFormat.WEBP, 10, ClubImagePurpose.CLUB, 7L, LocalDateTime.of(2026, 11, 28, 12, 0));

    @Test
    void 공개_주소_뒤에_저장_이름을_붙이고_비우면_API_경로를_쓴다() {
        assertThat(new ClubImageUrls("https://img.example.com").of(IMAGE))
                .isEqualTo("https://img.example.com/" + IMAGE.getStoredName());
        assertThat(new ClubImageUrls("https://img.example.com/").of(IMAGE))
                .isEqualTo("https://img.example.com/" + IMAGE.getStoredName());
        assertThat(new ClubImageUrls(" ").of(IMAGE)).isEqualTo("/api/clubs/images/" + IMAGE.getStoredName());
    }
}
