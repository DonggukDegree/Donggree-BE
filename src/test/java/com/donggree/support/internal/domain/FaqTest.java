package com.donggree.support.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.support.internal.domain.enums.FaqTag;
import org.junit.jupiter.api.Test;

class FaqTest {

    @Test
    void 생성_시_제목과_본문의_앞뒤_공백을_제거한다() {
        Faq faq = Faq.create(FaqTag.COMMON, "  성적표는 어디서 받나요?  ", "  nDRIMS에서 받을 수 있어요.  ");

        assertThat(faq.getTag()).isEqualTo(FaqTag.COMMON);
        assertThat(faq.getTitle()).isEqualTo("성적표는 어디서 받나요?");
        assertThat(faq.getContent()).isEqualTo("nDRIMS에서 받을 수 있어요.");
    }

    @Test
    void 본문의_줄바꿈은_그대로_보존한다() {
        Faq faq = Faq.create(FaqTag.COMMON, "제목", "첫째 줄\n둘째 줄");

        assertThat(faq.getContent()).isEqualTo("첫째 줄\n둘째 줄");
    }

    @Test
    void 수정하면_태그와_제목과_본문이_모두_교체된다() {
        Faq faq = Faq.create(FaqTag.COMMON, "원래 제목", "원래 본문");

        faq.update(FaqTag.MAJOR, "바뀐 제목", "바뀐 본문");

        assertThat(faq.getTag()).isEqualTo(FaqTag.MAJOR);
        assertThat(faq.getTitle()).isEqualTo("바뀐 제목");
        assertThat(faq.getContent()).isEqualTo("바뀐 본문");
    }

    @Test
    void 태그가_없으면_생성할_수_없다() {
        assertThatThrownBy(() -> Faq.create(null, "제목", "본문"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag");
    }

    @Test
    void 제목이_공백뿐이면_생성할_수_없다() {
        assertThatThrownBy(() -> Faq.create(FaqTag.COMMON, "   ", "본문"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void 본문이_공백뿐이면_생성할_수_없다() {
        assertThatThrownBy(() -> Faq.create(FaqTag.COMMON, "제목", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");
    }

    @Test
    void 제목이_200자를_넘으면_생성할_수_없다() {
        String tooLong = "가".repeat(201);

        assertThatThrownBy(() -> Faq.create(FaqTag.COMMON, tooLong, "본문"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void 수정_시에도_동일한_불변식을_검증한다() {
        Faq faq = Faq.create(FaqTag.COMMON, "제목", "본문");

        assertThatThrownBy(() -> faq.update(FaqTag.COMMON, "", "본문")).isInstanceOf(IllegalArgumentException.class);
        assertThat(faq.getTitle()).isEqualTo("제목");
    }
}
