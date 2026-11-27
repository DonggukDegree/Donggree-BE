package com.donggree.club.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 승인 없이 이 동아리를 바로 수정할 수 있는 회원. 이 동아리의 요청이 승인되면 생기고 관리자가 회수할 수 있다. */
@Entity
@Table(name = "club_editor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubEditor {

    @EmbeddedId
    private Key key;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    private ClubEditor(Long clubId, Long memberId, LocalDateTime grantedAt) {
        if (clubId == null || memberId == null || grantedAt == null) {
            throw new IllegalArgumentException("동아리·회원·부여 시각이 필요합니다");
        }
        this.key = new Key(clubId, memberId);
        this.grantedAt = grantedAt;
    }

    public static ClubEditor grant(Long clubId, Long memberId, LocalDateTime now) {
        return new ClubEditor(clubId, memberId, now);
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(name = "club_id")
        private Long clubId;

        @Column(name = "member_id")
        private Long memberId;

        public Key(Long clubId, Long memberId) {
            this.clubId = clubId;
            this.memberId = memberId;
        }
    }
}
