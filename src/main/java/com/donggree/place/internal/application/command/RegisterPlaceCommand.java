package com.donggree.place.internal.application.command;

import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import java.util.List;

public record RegisterPlaceCommand(
        String name,
        PlaceCategory category,
        PlaceSubCategory subCategory,
        double latitude,
        double longitude,
        String businessHours,
        String description,
        List<MenuItem> menus) {}
