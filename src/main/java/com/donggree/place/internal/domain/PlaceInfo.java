package com.donggree.place.internal.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 인증 회원 누구나 고칠 수 있는 장소 정보(영업시간·설명·메뉴). 메뉴는 입력 순서대로 전체를 담는다.
 *
 * @param businessHours 영업시간. 미입력이면 null
 * @param description   설명. 미입력이면 null
 * @param menus         메뉴 전체 목록
 */
public record PlaceInfo(String businessHours, String description, List<MenuItem> menus) {

    public PlaceInfo {
        // null 원소는 도메인 검증에서 IllegalArgumentException으로 막도록 그대로 담는다.
        menus = menus == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(menus));
    }
}
