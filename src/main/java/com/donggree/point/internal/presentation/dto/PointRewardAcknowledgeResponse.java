package com.donggree.point.internal.presentation.dto;

/**
 * 적립 안내 확인 결과.
 *
 * @param rewarded 이번에 처음 안내하는 적립이면 true. 적립이 없거나 이미 안내했거나 회수됐으면 false
 * @param amount   적립액. rewarded가 false면 0
 */
public record PointRewardAcknowledgeResponse(boolean rewarded, int amount) {}
