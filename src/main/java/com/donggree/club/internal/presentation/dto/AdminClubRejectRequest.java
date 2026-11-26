package com.donggree.club.internal.presentation.dto;

/** @param reason 반려 사유(1~200자). 요청자의 내 요청 내역에 그대로 보인다 */
public record AdminClubRejectRequest(String reason) {}
