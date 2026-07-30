package com.donggree.curriculum.internal.application.projection;

/**
 * 이수 영역 조회 응답. 과목 분류 필터의 선택지로 사용된다.
 */
public record AreaTypeProjection(Long id, String areaName) {}
