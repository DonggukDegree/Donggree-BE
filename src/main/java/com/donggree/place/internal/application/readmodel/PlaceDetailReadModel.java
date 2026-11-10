package com.donggree.place.internal.application.readmodel;

import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.time.LocalDateTime;
import java.util.List;

/** 장소 정보 화면용 상세. 등록자·수정자는 공개하지 않는다. */
public record PlaceDetailReadModel(
        Long id,
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        double latitude,
        double longitude,
        String businessHours,
        String description,
        List<Menu> menus,
        LocalDateTime createdAt,
        LocalDateTime infoUpdatedAt) {

    public static PlaceDetailReadModel from(Place place) {
        return new PlaceDetailReadModel(
                place.getId(),
                place.getName(),
                place.getCategory(),
                place.getSubCategory(),
                place.getLatitude(),
                place.getLongitude(),
                place.getBusinessHours(),
                place.getDescription(),
                place.getMenus().stream()
                        .map(menu -> new Menu(menu.getName(), menu.getPrice()))
                        .toList(),
                place.getCreatedAt(),
                place.getInfoUpdatedAt());
    }

    public record Menu(String name, Integer price) {}
}
