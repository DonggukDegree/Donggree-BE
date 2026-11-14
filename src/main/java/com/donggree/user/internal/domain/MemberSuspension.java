package com.donggree.user.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspensionSource;
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
 * 회원 정지 이력 한 건. 회원의 현재 정지 상태는 {@link Member#getSuspendedUntil()}에 두고, 여기에는 누가 언제
 * 어떤 근거로 며칠 정지했고 언제 해제했는지를 남긴다. 저장 후에는 해제 정보만 채운다.
 */
@Entity
@Table(
        name = "member_suspension",
        indexes = @Index(name = "idx_member_suspension_member_id", columnList = "member_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSuspension extends BaseEntity {

    public static final int NOTE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "days", nullable = false)
    private int days;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private SuspensionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 10)
    private ReportTargetType reportType;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "note", length = NOTE_MAX_LENGTH)
    private String note;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(name = "lifted_by")
    private Long liftedBy;

    /**
     * 지금부터 days일 정지 이력을 만든다. 업무 상한은 없고, 날짜 계산이 넘치는 값만 막는다.
     *
     * @throws IllegalArgumentException 1일 미만, 날짜 범위 초과, 메모 길이 초과, 신고 근거 누락
     */
    public static MemberSuspension start(
            Long memberId,
            int days,
            SuspensionSource source,
            ReportTargetType reportType,
            Long reportId,
            String note,
            Long adminId,
            LocalDateTime now) {
        if (memberId == null || source == null || adminId == null || now == null) {
            throw new IllegalArgumentException("memberId, source, adminId and now must not be null");
        }
        if (days < 1) {
            throw new IllegalArgumentException("days must be 1 or more");
        }
        LocalDateTime endsAt = now.plusDays(days);
        if (endsAt.getYear() > 9999) {
            throw new IllegalArgumentException("days is too large");
        }
        if (source == SuspensionSource.REPORT && (reportType == null || reportId == null)) {
            throw new IllegalArgumentException("report suspension needs reportType and reportId");
        }
        String normalized = note == null || note.isBlank() ? null : note.strip();
        if (normalized != null && normalized.length() > NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("note must be " + NOTE_MAX_LENGTH + " characters or less");
        }
        MemberSuspension suspension = new MemberSuspension();
        suspension.memberId = memberId;
        suspension.days = days;
        suspension.source = source;
        suspension.reportType = source == SuspensionSource.REPORT ? reportType : null;
        suspension.reportId = source == SuspensionSource.REPORT ? reportId : null;
        suspension.note = normalized;
        suspension.startsAt = now;
        suspension.endsAt = endsAt;
        suspension.createdBy = adminId;
        return suspension;
    }

    public boolean isActive(LocalDateTime now) {
        return liftedAt == null && endsAt.isAfter(now);
    }

    public void lift(Long adminId, LocalDateTime now) {
        this.liftedAt = now;
        this.liftedBy = adminId;
    }
}
