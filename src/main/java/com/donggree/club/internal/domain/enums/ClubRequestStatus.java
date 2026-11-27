package com.donggree.club.internal.domain.enums;

/** 요청 상태. 검토 중 요청을 고쳐 다시 보내면 기존 요청은 CANCELED가 된다. */
public enum ClubRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELED
}
