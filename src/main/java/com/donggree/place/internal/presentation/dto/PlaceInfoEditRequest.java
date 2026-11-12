package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.command.EditPlaceInfoCommand;
import com.donggree.place.internal.domain.enums.PlaceTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 장소 정보 수정 요청. 영업시간·설명·메뉴·숨은 맛집 태그 전체를 보내 교체한다.
 * 비우거나 null인 영업시간·설명은 삭제로 보고, 메뉴·태그는 빠뜨려 전체가 지워지지 않도록 필수로 받는다(빈 배열은 전체 삭제).
 */
public record PlaceInfoEditRequest(
        @NotNull(message = "버전은 필수입니다.")
                @Schema(example = "3", description = "상세 조회로 받은 장소 버전", requiredMode = Schema.RequiredMode.REQUIRED)
                Long version,
        @Size(max = 100, message = "영업시간은 100자 이하여야 합니다.") @Schema(example = "평일 11:00-21:00, 일요일 휴무")
                String businessHours,
        @Size(max = 300, message = "설명은 300자 이하여야 합니다.") @Schema(example = "정문 맞은편 골목 2층") String description,
        @NotNull(message = "메뉴 목록은 필수입니다.")
                @Size(max = 50, message = "메뉴는 50개 이하여야 합니다.")
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                @Valid
                List<@NotNull(message = "메뉴를 확인해주세요.") PlaceMenuRequest> menus,
        @NotNull(message = "숨은 맛집 태그 목록은 필수입니다.")
                @Size(max = 8, message = "숨은 맛집 태그를 확인해주세요.")
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "숨은 맛집 태그 전체(빈 배열이면 없음)")
                List<@NotNull(message = "숨은 맛집 태그를 확인해주세요.") PlaceTag> tags) {

    public EditPlaceInfoCommand toCommand() {
        return new EditPlaceInfoCommand(version, businessHours, description, PlaceMenuRequest.toItems(menus), tags);
    }
}
