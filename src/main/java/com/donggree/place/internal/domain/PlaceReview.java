package com.donggree.place.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 리뷰. 인증 회원이 장소마다 1건 쓰고(삭제 후 다시 작성 가능), 본인만 수정·삭제한다.
 *
 * <p>작성자 삭제는 행을 지우지 않고 삭제 시각만 남긴다. 신고 증거를 지우고 다시 쓰는 일을 막기 위해서다.
 * 회원당 장소별 1건은 삭제되지 않은 리뷰만 대상으로 DB 부분 유일 인덱스로도 보장한다.
 * 작성자는 회원 ID로만 참조하고 탈퇴 시 null로 익명화한다.
 */
@Entity
@Table(
        name = "place_review",
        indexes = {
            @Index(name = "idx_place_review_place_id", columnList = "place_id, id"),
            @Index(name = "idx_place_review_author_id", columnList = "author_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReview extends BaseEntity {

    public static final int RATING_MIN = 1;
    public static final int RATING_MAX = 5;
    public static final int CONTENT_MIN_LENGTH = 10;
    public static final int CONTENT_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    /** 작성 회원 ID. 탈퇴 시 null로 익명화한다. */
    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PlaceReviewStatus status;

    /** 작성자 삭제 시각. 삭제된 리뷰는 목록·평균·1건 제한에서 빠진다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private PlaceReview(Long placeId, Long authorId, int rating, String content) {
        this.placeId = placeId;
        this.authorId = authorId;
        this.rating = rating;
        this.content = content;
        this.status = PlaceReviewStatus.VISIBLE;
    }

    /** @throws IllegalArgumentException 필수값 누락, 별점 범위 밖, 본문 길이 오류 */
    public static PlaceReview write(Long placeId, Long authorId, int rating, String content) {
        if (placeId == null || authorId == null) {
            throw new IllegalArgumentException("placeId and authorId must not be null");
        }
        return new PlaceReview(placeId, authorId, requireRating(rating), requireContent(content));
    }

    /** 별점·본문을 고친다. 작성자 확인은 응용 서비스가 한다. */
    public void edit(int rating, String content) {
        this.rating = requireRating(rating);
        this.content = requireContent(content);
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void hide() {
        this.status = PlaceReviewStatus.HIDDEN;
    }

    public void show() {
        this.status = PlaceReviewStatus.VISIBLE;
    }

    public boolean isAuthor(Long memberId) {
        return authorId != null && authorId.equals(memberId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 목록에 보이는 리뷰. 숨기지 않았고 작성자가 지우지 않았다. */
    public boolean isVisible() {
        return status == PlaceReviewStatus.VISIBLE && !isDeleted();
    }

    private static int requireRating(int rating) {
        if (rating < RATING_MIN || rating > RATING_MAX) {
            throw new IllegalArgumentException("rating must be between " + RATING_MIN + " and " + RATING_MAX);
        }
        return rating;
    }

    private static String requireContent(String content) {
        String stripped = content == null ? "" : content.strip();
        if (stripped.length() < CONTENT_MIN_LENGTH || stripped.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "content must be " + CONTENT_MIN_LENGTH + " to " + CONTENT_MAX_LENGTH + " characters");
        }
        return stripped;
    }
}
