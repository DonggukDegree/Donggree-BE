package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.readmodel.PlaceDetailReadModel;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 장소 정보. 주소는 FE가 좌표로 실시간 조회하므로 포함하지 않는다. 버전은 정보 수정 요청에 돌려보낸다.
 * 등록자 닉네임이 null이면 FE가 "탈퇴한 회원"으로 표시한다.
 */
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
        LocalDateTime updatedAt,
        Long version,
        String registrantNickname,
        Double averageRating,
        long reviewCount,
        List<PlaceTag> tags) {

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
                place.infoUpdatedAt(),
                place.version(),
                place.registrantNickname(),
                place.averageRating(),
                place.reviewCount(),
                place.tags());
    }

    public record Menu(String name, Integer price) {}
}
