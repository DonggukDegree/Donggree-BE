package com.donggree.place.event;

/** 회원이 장소 정보를 실제로 바꿨다(변경 없는 요청·관리자 되돌리기는 발행하지 않음). */
public record PlaceInfoEditedEvent(Long placeId, Long memberId) {}
