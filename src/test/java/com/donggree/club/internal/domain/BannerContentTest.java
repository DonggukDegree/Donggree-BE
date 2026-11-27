package com.donggree.club.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BannerContentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final String GRADIENT = "linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)";

    @Test
    void 글자_배너는_배경이_필요하고_이미지_배너는_글자_칸을_비운다() {
        assertThatThrownBy(() -> text(null, "/clubs/register")).isInstanceOf(IllegalArgumentException.class);
        BannerContent image = new BannerContent(
                "광고 이미지", "AD", "설명", GRADIENT, "/api/clubs/images/a.webp", null, "/clubs", null, null, true);

        assertThat(image.eyebrow()).isNull();
        assertThat(image.description()).isNull();
        assertThat(image.background()).isNull();
    }

    @Test
    void 링크와_이미지는_동그리_경로나_https만_받고_배경은_CSS_값만_받는다() {
        assertThat(text(GRADIENT, "/clubs?club=3").link()).isEqualTo("/clubs?club=3");
        assertThat(text(GRADIENT, "https://pf.kakao.com/_x/chat").link()).startsWith("https://");
        assertThatThrownBy(() -> text(GRADIENT, "http://pf.kakao.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> text(GRADIENT, "//evil.example.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> text(GRADIENT, "javascript:alert(1)")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> text("url(https://x.com/a.png); color: red", "/clubs"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 켜져_있고_노출_기간_안일_때만_공개한다() {
        BannerContent scheduled = new BannerContent(
                "이벤트", null, null, GRADIENT, null, null, "/clubs", NOW.plusDays(1), NOW.plusDays(3), true);
        BannerContent off = new BannerContent("이벤트", null, null, GRADIENT, null, null, "/clubs", null, null, false);

        assertThat(scheduled.visibleAt(NOW)).isFalse();
        assertThat(scheduled.visibleAt(NOW.plusDays(1))).isTrue();
        assertThat(scheduled.visibleAt(NOW.plusDays(3))).isFalse();
        assertThat(off.visibleAt(NOW)).isFalse();
        assertThatThrownBy(() -> new BannerContent("이벤트", null, null, GRADIENT, null, null, "/clubs", NOW, NOW, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BannerContent text(String background, String link) {
        return new BannerContent("우리 동아리가 없나요?", "EVENT", "등록하면 500P", background, null, null, link, null, null, true);
    }
}
