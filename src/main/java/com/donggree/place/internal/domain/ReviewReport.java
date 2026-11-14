package com.donggree.place.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
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
 * 리뷰 신고. 장소 신고와 같은 규칙: 회원당 리뷰별 처리 대기 1건, 서로 다른 회원 3명의 대기 신고 시 리뷰 자동 숨김,
 * 그 밖의 처리는 관리자. 본인 리뷰는 신고할 수 없다. 신고자는 탈퇴 시 null로 익명화한다.
 */
@Entity
@Table(
        name = "place_review_report",
        indexes = {
            @Index(name = "idx_place_review_report_review_id", columnList = "review_id, status"),
            @Index(name = "idx_place_review_report_status", columnList = "status, created_at"),
            @Index(name = "idx_place_review_report_reporter_id", columnList = "reporter_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport extends BaseEntity {

    public static final int DESCRIPTION_MAX_LENGTH = 200;
    public static final int AUTO_HIDE_PENDING_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    /** 신고 회원 ID. 탈퇴 시 null로 익명화한다. */
    @Column(name = "reporter_id")
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private ReviewReportReason reason;

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PlaceReportStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private ReviewReport(Long reviewId, Long reporterId, ReviewReportReason reason, String description) {
        this.reviewId = reviewId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.description = description;
        this.status = PlaceReportStatus.PENDING;
    }

    /** @throws IllegalArgumentException 필수값 누락, 설명 길이 초과, 기타 사유의 설명 누락 */
    public static ReviewReport submit(Long reviewId, Long reporterId, ReviewReportReason reason, String description) {
        if (reviewId == null || reporterId == null || reason == null) {
            throw new IllegalArgumentException("reviewId, reporterId and reason must not be null");
        }
        String normalized = description == null || description.isBlank() ? null : description.strip();
        if (normalized != null && normalized.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be " + DESCRIPTION_MAX_LENGTH + " characters or less");
        }
        if (reason == ReviewReportReason.OTHER && normalized == null) {
            throw new IllegalArgumentException("description is required for OTHER reason");
        }
        return new ReviewReport(reviewId, reporterId, reason, normalized);
    }

    public boolean isPending() {
        return status == PlaceReportStatus.PENDING;
    }

    /** 관리자가 신고를 인정해 처리 완료한다(숨김·수정·정지 등 조치 후). */
    public void resolve(LocalDateTime now) {
        process(PlaceReportStatus.RESOLVED, now);
    }

    /** 관리자가 신고를 받아들이지 않고 기각한다. */
    public void reject(LocalDateTime now) {
        process(PlaceReportStatus.REJECTED, now);
    }

    private void process(PlaceReportStatus next, LocalDateTime now) {
        if (status != PlaceReportStatus.PENDING) {
            throw new IllegalStateException("only pending report can be processed");
        }
        this.status = next;
        this.processedAt = java.util.Objects.requireNonNull(now);
    }
}
