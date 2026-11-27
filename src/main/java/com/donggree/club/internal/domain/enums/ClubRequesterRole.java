package com.donggree.club.internal.domain.enums;

/** 요청자가 동아리와 어떤 관계인지(승인 검토·추적용, 관리자만 본다). */
public enum ClubRequesterRole {
    /** 회장·부회장 */
    PRESIDENT,
    /** 운영진 */
    STAFF,
    /** 일반 부원 */
    MEMBER,
    /** 외부인 */
    OUTSIDER
}
