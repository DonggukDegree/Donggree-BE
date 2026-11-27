package com.donggree.club.internal.domain;

import com.donggree.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 동아리 쇼핑 목록 상단 광고 배너. sortOrder가 작을수록 먼저 보인다. */
@Entity
@Table(name = "club_banner", indexes = @Index(name = "idx_club_banner_sort_order", columnList = "sort_order, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubBanner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = BannerContent.TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "eyebrow", length = BannerContent.EYEBROW_MAX_LENGTH)
    private String eyebrow;

    @Column(name = "description", length = BannerContent.DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "background", length = BannerContent.BACKGROUND_MAX_LENGTH)
    private String background;

    @Column(name = "image_url", length = BannerContent.URL_MAX_LENGTH)
    private String imageUrl;

    @Column(name = "mobile_image_url", length = BannerContent.URL_MAX_LENGTH)
    private String mobileImageUrl;

    @Column(name = "link", nullable = false, length = BannerContent.URL_MAX_LENGTH)
    private String link;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private ClubBanner(BannerContent content, int sortOrder) {
        apply(content);
        this.sortOrder = sortOrder;
    }

    /** 맨 뒤 순서로 만든다. */
    public static ClubBanner create(BannerContent content, int sortOrder) {
        if (content == null) throw new IllegalArgumentException("배너 내용이 필요합니다");
        return new ClubBanner(content, sortOrder);
    }

    public void revise(BannerContent content) {
        if (content == null) throw new IllegalArgumentException("배너 내용이 필요합니다");
        apply(content);
    }

    /** 다른 배너와 노출 순서를 맞바꾼다. */
    public void swapOrder(ClubBanner other) {
        int mine = sortOrder;
        this.sortOrder = other.sortOrder;
        other.sortOrder = mine;
    }

    public BannerContent content() {
        return new BannerContent(
                title, eyebrow, description, background, imageUrl, mobileImageUrl, link, startsAt, endsAt, active);
    }

    private void apply(BannerContent content) {
        this.title = content.title();
        this.eyebrow = content.eyebrow();
        this.description = content.description();
        this.background = content.background();
        this.imageUrl = content.imageUrl();
        this.mobileImageUrl = content.mobileImageUrl();
        this.link = content.link();
        this.startsAt = content.startsAt();
        this.endsAt = content.endsAt();
        this.active = content.active();
    }
}
