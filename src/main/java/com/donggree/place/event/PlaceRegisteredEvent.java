package com.donggree.place.event;

/** 회원이 장소를 새로 등록했다. 등록 트랜잭션이 커밋된 뒤 전달된다. */
public record PlaceRegisteredEvent(Long placeId, Long memberId) {}
