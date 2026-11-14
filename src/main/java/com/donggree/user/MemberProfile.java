package com.donggree.user;

/**
 * 다른 모듈에 공개하는 회원 표시 정보. 작성자 표시용으로만 쓰고 복사 저장하지 않는다.
 *
 * @param memberId   회원 ID
 * @param nickname   닉네임. 온보딩 전이면 null
 * @param profileUrl 프로필 사진 주소. 아직 수집하지 않아 대부분 null이며, 없으면 기본 아바타를 쓴다
 */
public record MemberProfile(Long memberId, String nickname, String profileUrl) {}
