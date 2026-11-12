package com.donggree.place.internal.application.command;

import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.util.List;

public record RegisterPlaceCommand(
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        double latitude,
        double longitude,
        String businessHours,
        String description,
        List<MenuItem> menus,
        List<PlaceTag> tags) {

    /** 숨은 맛집 태그 없이 등록한다. */
    public RegisterPlaceCommand(
            String name,
            PlaceCategory category,
            PlaceSubCategory subCategory,
            double latitude,
            double longitude,
            String businessHours,
            String description,
            List<MenuItem> menus) {
        this(name, category, subCategory, latitude, longitude, businessHours, description, menus, List.of());
    }
}
