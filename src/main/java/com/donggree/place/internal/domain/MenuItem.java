package com.donggree.place.internal.domain;

/**
 * 장소 등록·수정 시 입력받는 메뉴 한 줄. 가격이 없으면 "가격 변동"으로 표시한다.
 *
 * @param name  메뉴명
 * @param price 가격(원). null이면 가격 변동
 */
public record MenuItem(String name, Integer price) {}
