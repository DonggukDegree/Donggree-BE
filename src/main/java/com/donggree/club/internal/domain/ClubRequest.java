package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import com.donggree.global.entity.BaseEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 관리자 승인이 필요한 동아리 신규 등록·정보 수정·모집 등록 요청.
 *
 * <p>요청 당시의 현재 값(before)과 요청 값(after)을 그대로 남겨 관리자가 비교한다. 요청자는 회원 ID로만 참조하고,
 * 연락처·동아리와의 관계는 보유 기간이 지나거나 탈퇴하면 비운다. 검토 중 요청을 고쳐 다시 보내면 취소하고 새 요청을 만든다.
 */
@Entity
@Table(name = "club_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubRequest extends BaseEntity {

    public static final int REJECT_REASON_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 15)
    private ClubRequestType type;

    /** 대상 동아리. 신규 등록은 승인 전까지 null이다. */
    @Column(name = "club_id")
    private Long clubId;

    /** 요청 당시 동아리 이름(신규 등록은 요청한 이름). */
    @Column(name = "club_name", nullable = false, length = ClubProfile.NAME_MAX_LENGTH)
    private String clubName;

    /** 요청 회원 ID. 탈퇴 시 null. */
    @Column(name = "requester_id")
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_role", length = 15)
    private ClubRequesterRole requesterRole;

    @Column(name = "requester_contact", length = ClubRequester.CONTACT_MAX_LENGTH)
    private String requesterContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ClubRequestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_value", columnDefinition = "jsonb")
    private ClubRequestPayload before;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_value", nullable = false, columnDefinition = "jsonb")
    private ClubRequestPayload after;

    @Column(name = "reject_reason", length = REJECT_REASON_MAX_LENGTH)
    private String rejectReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private ClubRequest(
            ClubRequestType type,
            Long clubId,
            String clubName,
            Long requesterId,
            ClubRequester requester,
            ClubRequestPayload before,
            ClubRequestPayload after) {
        if (requesterId == null || requester == null || after == null) {
            throw new IllegalArgumentException("요청자와 요청 값이 필요합니다");
        }
        if (type != ClubRequestType.REGISTER && clubId == null) {
            throw new IllegalArgumentException("수정·모집 요청은 대상 동아리가 필요합니다");
        }
        boolean recruitment = type == ClubRequestType.RECRUITMENT;
        if ((after.recruitment() != null) != recruitment
                || (before != null && (before.recruitment() != null) != recruitment)) {
            throw new IllegalArgumentException("요청 종류와 값이 맞지 않습니다");
        }
        this.type = type;
        this.clubId = clubId;
        this.clubName = clubName;
        this.requesterId = requesterId;
        this.requesterRole = requester.role();
        this.requesterContact = requester.contact();
        this.status = ClubRequestStatus.PENDING;
        this.before = before;
        this.after = after;
    }

    public static ClubRequest register(Long requesterId, ClubRequester requester, ClubRequestPayload after) {
        return new ClubRequest(
                ClubRequestType.REGISTER, null, after.profile().name(), requesterId, requester, null, after);
    }

    public static ClubRequest edit(
            Club club, Long requesterId, ClubRequester requester, ClubRequestPayload before, ClubRequestPayload after) {
        return new ClubRequest(
                ClubRequestType.EDIT, club.getId(), club.getName(), requesterId, requester, before, after);
    }

    /** @param before 고쳐 쓸 진행 중·예정 모집. 새 모집이면 null */
    public static ClubRequest recruitment(
            Club club, Long requesterId, ClubRequester requester, ClubRequestPayload before, ClubRequestPayload after) {
        return new ClubRequest(
                ClubRequestType.RECRUITMENT, club.getId(), club.getName(), requesterId, requester, before, after);
    }

    public boolean isRequestedBy(Long memberId) {
        return requesterId != null && requesterId.equals(memberId);
    }

    /** 검토 중·반려된 요청만 고쳐 다시 보낼 수 있다. */
    public boolean resubmittable() {
        return status == ClubRequestStatus.PENDING || status == ClubRequestStatus.REJECTED;
    }

    /** 다시 요청으로 검토 중 요청을 취소한다. 반려된 요청은 그대로 둔다. */
    public void cancelForResubmit(LocalDateTime now) {
        if (!resubmittable()) {
            throw new IllegalStateException("검토 중·반려된 요청만 다시 보낼 수 있습니다");
        }
        if (status != ClubRequestStatus.PENDING) return;
        this.status = ClubRequestStatus.CANCELED;
        this.processedAt = now;
    }
}
