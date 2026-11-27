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

/** 회원이 찜한 동아리. */
@Entity
@Table(name = "club_favorite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubFavorite {

    @EmbeddedId
    private Key key;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ClubFavorite(Long memberId, Long clubId, LocalDateTime createdAt) {
        if (memberId == null || clubId == null || createdAt == null) {
            throw new IllegalArgumentException("회원·동아리·찜한 시각이 필요합니다");
        }
        this.key = new Key(memberId, clubId);
        this.createdAt = createdAt;
    }

    public static ClubFavorite of(Long memberId, Long clubId, LocalDateTime now) {
        return new ClubFavorite(memberId, clubId, now);
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "club_id")
        private Long clubId;

        public Key(Long memberId, Long clubId) {
            this.memberId = memberId;
            this.clubId = clubId;
        }
    }
}
