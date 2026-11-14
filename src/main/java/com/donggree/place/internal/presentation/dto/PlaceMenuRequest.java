package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.domain.MenuItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 장소 등록·수정 요청의 메뉴 한 줄. 목록 순서대로 저장한다. */
public record PlaceMenuRequest(
        @NotBlank(message = "메뉴명은 필수입니다.") @Size(max = 30, message = "메뉴명은 30자 이하여야 합니다.") @Schema(example = "짜장면")
                String name,
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
                @Max(value = 1_000_000, message = "가격은 1,000,000원 이하여야 합니다.")
                @Schema(example = "7000", description = "가격(원). 비우면 가격 변동")
                Integer price) {

    static List<MenuItem> toItems(List<PlaceMenuRequest> menus) {
        return menus == null
                ? List.of()
                : menus.stream()
                        .map(menu -> new MenuItem(menu.name(), menu.price()))
                        .toList();
    }
}
