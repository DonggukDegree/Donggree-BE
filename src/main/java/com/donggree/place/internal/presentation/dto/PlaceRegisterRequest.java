package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.command.RegisterPlaceCommand;
import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 장소 등록 요청. 위치는 지도 중심 핀 좌표이며 주소는 받지 않는다(카카오 주소 저장 금지).
 * 카테고리 짝·서비스 영역·중복 여부는 서버에서 다시 검증한다.
 */
public record PlaceRegisterRequest(
        @NotBlank(message = "이름은 필수입니다.")
                @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
                @Schema(example = "동국반점", requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @NotNull(message = "카테고리는 필수입니다.") @Schema(example = "RESTAURANT", requiredMode = Schema.RequiredMode.REQUIRED)
                PlaceCategory category,
        @NotNull(message = "상세카테고리는 필수입니다.") @Schema(example = "CHINESE", requiredMode = Schema.RequiredMode.REQUIRED)
                PlaceSubCategory subCategory,
        @NotNull(message = "위도는 필수입니다.")
                @DecimalMin(value = "-90", message = "위도를 확인해주세요.")
                @DecimalMax(value = "90", message = "위도를 확인해주세요.")
                @Schema(example = "37.5585", requiredMode = Schema.RequiredMode.REQUIRED)
                Double latitude,
        @NotNull(message = "경도는 필수입니다.")
                @DecimalMin(value = "-180", message = "경도를 확인해주세요.")
                @DecimalMax(value = "180", message = "경도를 확인해주세요.")
                @Schema(example = "126.9990", requiredMode = Schema.RequiredMode.REQUIRED)
                Double longitude,
        @Size(max = 100, message = "영업시간은 100자 이하여야 합니다.") @Schema(example = "평일 11:00-21:00, 일요일 휴무")
                String businessHours,
        @Size(max = 300, message = "설명은 300자 이하여야 합니다.") @Schema(example = "정문 맞은편 골목 2층") String description,
        @Size(max = 50, message = "메뉴는 50개 이하여야 합니다.") @Valid List<Menu> menus) {

    public RegisterPlaceCommand toCommand() {
        return new RegisterPlaceCommand(
                name,
                category,
                subCategory,
                latitude,
                longitude,
                businessHours,
                description,
                menus == null
                        ? List.of()
                        : menus.stream()
                                .map(menu -> new MenuItem(menu.name(), menu.price()))
                                .toList());
    }

    public record Menu(
            @NotBlank(message = "메뉴명은 필수입니다.") @Size(max = 30, message = "메뉴명은 30자 이하여야 합니다.") @Schema(example = "짜장면")
                    String name,
            @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
                    @Max(value = 1_000_000, message = "가격은 1,000,000원 이하여야 합니다.")
                    @Schema(example = "7000", description = "가격(원). 비우면 가격 변동")
                    Integer price) {}
}
