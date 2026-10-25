package com.donggree.place.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 장소 메뉴 한 줄. 장소 애그리거트 안에서만 생성·교체한다. */
@Entity
@Table(name = "place_menu", indexes = @Index(name = "idx_place_menu_place_id", columnList = "place_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceMenu {

    static final int NAME_MAX_LENGTH = 30;
    static final int PRICE_MAX = 1_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    PlaceMenu(Place place, MenuItem item, int sortOrder) {
        if (item == null) {
            throw new IllegalArgumentException("menu must not be null");
        }
        String validatedName = item.name() == null ? "" : item.name().trim();
        if (validatedName.isEmpty() || validatedName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("menu name must be 1 to " + NAME_MAX_LENGTH + " characters");
        }
        if (item.price() != null && (item.price() < 0 || item.price() > PRICE_MAX)) {
            throw new IllegalArgumentException("menu price must be between 0 and " + PRICE_MAX);
        }
        this.place = place;
        this.name = validatedName;
        this.price = item.price();
        this.sortOrder = sortOrder;
    }
}
