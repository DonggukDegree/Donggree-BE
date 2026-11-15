package com.donggree.point.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 적립 한 건. 적립은 {@link PointHistoryRepository#grant}의 중복 무시 INSERT로만 만든다.
 * 대상이 숨겨지면 revokedAt을 기록해 합계에서 빼고(회수), 숨김이 풀리면 지워 다시 더한다.
 * notifiedAt은 화면이 적립 안내(토스트 등)를 띄운 시각이다.
 */
@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private PointReason reason;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
