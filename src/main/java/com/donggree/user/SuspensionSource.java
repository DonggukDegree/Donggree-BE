package com.donggree.user;

/** 정지 사유 구분. 신고 처리로 인한 정지(REPORT)와 관리자 판단 정지(ADMIN). 안내 문구가 다르다. */
public enum SuspensionSource {
    REPORT,
    ADMIN
}
