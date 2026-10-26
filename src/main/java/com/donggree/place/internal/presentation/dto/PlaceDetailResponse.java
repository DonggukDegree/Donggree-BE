package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.time.LocalDateTime;
import java.util.List;

/** 장소 정보. 주소는 FE가 좌표로 실시간 조회하므로 포함하지 않는다. */
public record PlaceDetailResponse(
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
        LocalDateTime updatedAt) {

    public static PlaceDetailResponse from(PlaceDetailReadModel place) {
        return new PlaceDetailResponse(
                place.id(),
                place.name(),
                place.category(),
                place.subCategory(),
                place.latitude(),
                place.longitude(),
                place.businessHours(),
                place.description(),
                place.menus().stream()
                        .map(menu -> new Menu(menu.name(), menu.price()))
                        .toList(),
                place.createdAt(),
                place.infoUpdatedAt());
    }

    public record Menu(String name, Integer price) {}
}
