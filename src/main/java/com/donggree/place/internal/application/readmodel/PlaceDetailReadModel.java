package com.donggree.place.internal.application.readmodel;

import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 장소 정보 화면용 상세. 최초 등록자는 닉네임만 공개하고(탈퇴 등으로 없으면 null), 수정자는 공개하지 않는다.
 * 버전은 정보 수정 요청에 그대로 돌려받는다.
 */
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
        LocalDateTime infoUpdatedAt,
        Long version,
        String registrantNickname) {

    public static PlaceDetailReadModel from(Place place, String registrantNickname) {
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
                place.getInfoUpdatedAt(),
                place.getVersion(),
                registrantNickname);
    }

    public record Menu(String name, Integer price) {}
}
