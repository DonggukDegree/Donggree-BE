package com.donggree.user;

import java.util.List;

/** 관리자 회원 검색 한 페이지. */
public record MemberSearchPage(List<MemberAdminProfile> members, long totalCount, int page, int size) {}
