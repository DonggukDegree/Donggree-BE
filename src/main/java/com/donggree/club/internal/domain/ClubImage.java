package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 올린 사진 한 장의 메타데이터. 파일은 저장소에 storedName(추측할 수 없는 이름)으로 둔다.
 *
 * <p>동아리 사진은 승인·바로 반영 때 동아리에 순서·대표 여부와 함께 연결되고, 빠지면 연결을 끊는다(파일은 정리 작업이 지운다).
 */
@Entity
@Table(name = "club_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stored_name", nullable = false, length = 60, unique = true)
    private String storedName;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 10)
    private ClubImagePurpose purpose;

    /** 올린 회원 ID. 관리자가 올렸거나 탈퇴했으면 null일 수 있다. */
    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "representative", nullable = false)
    private boolean representative;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ClubImage(ImageFormat format, int sizeBytes, ClubImagePurpose purpose, Long uploaderId, LocalDateTime now) {
        if (format == null || purpose == null || now == null) {
            throw new IllegalArgumentException("사진 형식·쓰임·시각이 필요합니다");
        }
        if (sizeBytes < 1 || sizeBytes > ImageFormat.MAX_BYTES) {
            throw new IllegalArgumentException("사진은 5MB 이하만 올릴 수 있습니다");
        }
        this.storedName = UUID.randomUUID() + "." + format.extension();
        this.contentType = format.contentType();
        this.sizeBytes = sizeBytes;
        this.purpose = purpose;
        this.uploaderId = uploaderId;
        this.createdAt = now;
    }

    public static ClubImage upload(
            ImageFormat format, int sizeBytes, ClubImagePurpose purpose, Long uploaderId, LocalDateTime now) {
        return new ClubImage(format, sizeBytes, purpose, uploaderId, now);
    }

    /** 이 회원이 동아리 사진으로 쓸 수 있는지: 본인이 올렸거나 이미 이 동아리 사진이다. */
    public boolean usableBy(Long memberId, Long targetClubId) {
        if (purpose != ClubImagePurpose.CLUB) return false;
        if (targetClubId != null && targetClubId.equals(clubId)) return true;
        return clubId == null && uploaderId != null && uploaderId.equals(memberId);
    }

    public void attach(Long clubId, int sortOrder, boolean representative) {
        if (clubId == null) throw new IllegalArgumentException("연결할 동아리가 필요합니다");
        this.clubId = clubId;
        this.sortOrder = sortOrder;
        this.representative = representative;
    }

    public void detach() {
        this.clubId = null;
        this.sortOrder = null;
        this.representative = false;
    }
}
