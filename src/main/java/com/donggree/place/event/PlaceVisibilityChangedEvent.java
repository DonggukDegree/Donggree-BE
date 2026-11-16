package com.donggree.place.event;

/** 관리자 처리나 신고 누적으로 장소가 숨겨지거나(visible=false) 다시 보이게 됐다. 이미 같은 상태여도 발행한다. */
public record PlaceVisibilityChangedEvent(Long placeId, boolean visible) {}
