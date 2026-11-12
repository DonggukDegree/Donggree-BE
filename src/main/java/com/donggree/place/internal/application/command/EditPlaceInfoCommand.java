package com.donggree.place.internal.application.command;

import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.util.List;

/**
 * 장소 정보 수정 명령. 영업시간·설명·메뉴·숨은 맛집 태그 전체를 받아 교체한다.
 *
 * @param version 수정 화면을 연 시점의 장소 버전. 현재 버전과 다르면 충돌로 막는다
 */
public record EditPlaceInfoCommand(
        Long version, String businessHours, String description, List<MenuItem> menus, List<PlaceTag> tags) {

    /** 태그를 비운 수정 명령(테스트 편의). */
    public EditPlaceInfoCommand(Long version, String businessHours, String description, List<MenuItem> menus) {
        this(version, businessHours, description, menus, List.of());
    }
}
