package com.donggree.place.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
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
 * 장소 신고. 등록자를 포함한 인증 회원 누구나 제출하고, 관리자가 처리한다.
 *
 * <p>회원당 장소별 처리 대기 신고는 1건이며, 처리 후에는 다시 신고할 수 있다(DB 부분 유일 인덱스로도 막는다).
 * 서로 다른 회원의 처리 대기 신고가 {@link #AUTO_HIDE_PENDING_COUNT}건 쌓이면 장소를 자동으로 숨기고,
 * 신고는 관리자 확인을 위해 대기로 남긴다. 그 밖의 처리는 관리자가 한다.
 * 장소와는 ID로만 연결하고, 신고자는 회원 ID로만 참조하며 탈퇴 시 null로 익명화한다.
 */
@Entity
@Table(
        name = "place_report",
        indexes = {
            @Index(name = "idx_place_report_place_id", columnList = "place_id, status"),
            @Index(name = "idx_place_report_status", columnList = "status, created_at"),
            @Index(name = "idx_place_report_reporter_id", columnList = "reporter_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReport extends BaseEntity {

    public static final int DESCRIPTION_MAX_LENGTH = 200;
    public static final int AUTO_HIDE_PENDING_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    /** 신고 회원 ID. 탈퇴 시 null로 익명화한다. */
    @Column(name = "reporter_id")
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private PlaceReportReason reason;

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PlaceReportStatus status;

    /** 처리(완료·기각) 시각. 대기 중이면 null. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private PlaceReport(Long placeId, Long reporterId, PlaceReportReason reason, String description) {
        this.placeId = placeId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.description = description;
        this.status = PlaceReportStatus.PENDING;
    }

    /**
     * 처리 대기 신고를 만든다. 공백만 입력한 설명은 미입력으로 보며, 기타 사유는 설명이 있어야 한다.
     *
     * @throws IllegalArgumentException 필수값 누락, 설명 길이 초과, 기타 사유의 설명 누락
     */
    public static PlaceReport submit(Long placeId, Long reporterId, PlaceReportReason reason, String description) {
        if (placeId == null || reporterId == null || reason == null) {
            throw new IllegalArgumentException("placeId, reporterId and reason must not be null");
        }
        String normalized = description == null || description.isBlank() ? null : description.strip();
        if (normalized != null && normalized.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be " + DESCRIPTION_MAX_LENGTH + " characters or less");
        }
        if (reason == PlaceReportReason.OTHER && normalized == null) {
            throw new IllegalArgumentException("description is required for OTHER reason");
        }
        return new PlaceReport(placeId, reporterId, reason, normalized);
    }

    public boolean isPending() {
        return status == PlaceReportStatus.PENDING;
    }
}
