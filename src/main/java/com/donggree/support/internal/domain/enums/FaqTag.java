package com.donggree.support.internal.domain.enums;

/**
 * FAQ 글의 태그. 사용자 화면 상단의 칩으로 노출되어 목록을 걸러내는 데 쓰인다.
 * 항목이 늘어날 수 있으므로 화면 문구는 프론트가 들고 있고, 서버는 enum 이름만 주고받는다.
 */
public enum FaqTag {
    /** 동그리 서비스 자체에 대한 질문(가입·업로드·학업정보수정 등). */
    SERVICE,
    /** 공통 졸업 요건(교양·학문기초 등)에 대한 질문. */
    COMMON,
    /** 전공 졸업 요건에 대한 질문. */
    MAJOR
}
