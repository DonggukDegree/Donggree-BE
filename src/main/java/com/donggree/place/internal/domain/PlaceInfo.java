package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceTag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 인증 회원 누구나 고칠 수 있는 장소 정보(영업시간·설명·메뉴·숨은 맛집 태그). 메뉴는 입력 순서대로 전체를 담는다.
 *
 * @param businessHours 영업시간. 미입력이면 null
 * @param description   설명. 미입력이면 null
 * @param menus         메뉴 전체 목록
 * @param tags          숨은 맛집 태그. 0개 이상, 정규화하면 중복 없이 enum 순서
 */
public record PlaceInfo(String businessHours, String description, List<MenuItem> menus, List<PlaceTag> tags) {

    public PlaceInfo {
        // null 원소는 도메인 검증에서 IllegalArgumentException으로 막도록 그대로 담는다.
        menus = menus == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(menus));
        tags = tags == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(tags));
    }

    /** 태그 없는 장소 정보. */
    public PlaceInfo(String businessHours, String description, List<MenuItem> menus) {
        this(businessHours, description, menus, List.of());
    }
}
