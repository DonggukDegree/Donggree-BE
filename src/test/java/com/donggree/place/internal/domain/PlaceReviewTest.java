package com.donggree.place.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PlaceReviewTest {

    @Test
    void 리뷰는_보이는_상태로_작성하고_본문_앞뒤_공백을_지운다() {
        PlaceReview review = PlaceReview.write(1L, 7L, 4, "  짜장면이 맛있고 양이 많아요  ");

        assertThat(review.getStatus()).isEqualTo(PlaceReviewStatus.VISIBLE);
        assertThat(review.getContent()).isEqualTo("짜장면이 맛있고 양이 많아요");
        assertThat(review.isVisible()).isTrue();
        assertThat(review.isAuthor(7L)).isTrue();
        assertThat(review.isAuthor(8L)).isFalse();
    }

    @Test
    void 별점은_1에서_5_본문은_공백_제외_10자에서_500자까지_허용한다() {
        assertThatThrownBy(() -> PlaceReview.write(1L, 7L, 0, "가".repeat(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReview.write(1L, 7L, 6, "가".repeat(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReview.write(1L, 7L, 3, "  " + "가".repeat(9) + "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReview.write(1L, 7L, 3, "가".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReview.write(1L, null, 3, "가".repeat(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PlaceReview.write(1L, 7L, 5, "가".repeat(500)).getContent()).hasSize(500);
        assertThat(PlaceReview.write(1L, 7L, 1, "가".repeat(10)).getRating()).isEqualTo(1);
    }

    @Test
    void 수정은_같은_규칙을_검사하고_실패하면_기존_값을_유지한다() {
        PlaceReview review = PlaceReview.write(1L, 7L, 4, "짜장면이 맛있고 양이 많아요");

        assertThatThrownBy(() -> review.edit(6, "탕수육도 바삭하고 맛있어요")).isInstanceOf(IllegalArgumentException.class);
        assertThat(review.getRating()).isEqualTo(4);
        review.edit(5, "탕수육도 바삭하고 맛있어요");
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("탕수육도 바삭하고 맛있어요");
    }

    @Test
    void 숨김과_삭제는_목록에서_뺀다() {
        PlaceReview review = PlaceReview.write(1L, 7L, 4, "짜장면이 맛있고 양이 많아요");

        review.hide();
        assertThat(review.isVisible()).isFalse();
        review.show();
        assertThat(review.isVisible()).isTrue();
        review.delete(LocalDateTime.now());
        assertThat(review.isDeleted()).isTrue();
        assertThat(review.isVisible()).isFalse();
    }

    @Test
    void 리뷰_신고는_기타_사유에_설명이_필요하고_200자까지_허용한다() {
        assertThat(ReviewReport.submit(1L, 8L, ReviewReportReason.ABUSE, " ").getDescription())
                .isNull();
        assertThatThrownBy(() -> ReviewReport.submit(1L, 8L, ReviewReportReason.OTHER, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewReport.submit(1L, 8L, ReviewReportReason.SPAM, "가".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewReport.submit(1L, 8L, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
