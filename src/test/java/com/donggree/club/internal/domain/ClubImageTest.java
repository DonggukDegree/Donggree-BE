package com.donggree.club.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ClubImageTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);

    @Test
    void 형식은_파일_앞부분으로_판단한다() {
        assertThat(ImageFormat.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0))).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)))
                .contains(ImageFormat.PNG);
        assertThat(ImageFormat.detect(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P')))
                .contains(ImageFormat.WEBP);
        assertThat(ImageFormat.detect(bytes('G', 'I', 'F', '8', '9', 'a'))).isEmpty();
        assertThat(ImageFormat.detect("<svg onload=alert(1)>".getBytes())).isEmpty();
    }

    @Test
    void 추측할_수_없는_이름으로_저장하고_5MB를_넘으면_거절한다() {
        ClubImage image = ClubImage.upload(ImageFormat.WEBP, 1024, ClubImagePurpose.CLUB, 7L, NOW);

        assertThat(image.getStoredName()).matches("[0-9a-f-]{36}\\.webp");
        assertThat(image.url()).isEqualTo("/api/clubs/images/" + image.getStoredName());
        assertThatThrownBy(() ->
                        ClubImage.upload(ImageFormat.PNG, ImageFormat.MAX_BYTES + 1, ClubImagePurpose.CLUB, 7L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 직접_올린_연결_안_된_사진이나_그_동아리_사진만_쓸_수_있다() {
        ClubImage mine = ClubImage.upload(ImageFormat.PNG, 10, ClubImagePurpose.CLUB, 7L, NOW);
        ClubImage banner = ClubImage.upload(ImageFormat.PNG, 10, ClubImagePurpose.BANNER, 7L, NOW);

        assertThat(mine.usableBy(7L, null)).isTrue();
        assertThat(mine.usableBy(8L, null)).isFalse();
        assertThat(banner.usableBy(7L, null)).isFalse();

        mine.attach(3L, 0, true);
        assertThat(mine.usableBy(8L, 3L)).isTrue();
        assertThat(mine.usableBy(7L, 4L)).isFalse();
        mine.detach();
        assertThat(mine.isRepresentative()).isFalse();
        assertThat(mine.getSortOrder()).isNull();
    }

    private static byte[] bytes(int... values) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < values.length; i++) bytes[i] = (byte) values[i];
        return bytes;
    }
}
