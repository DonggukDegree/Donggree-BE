package com.donggree.place.internal.application.readmodel;

import java.time.LocalDateTime;

/**
 * 리뷰 한 건. 작성자 닉네임이 null이면 탈퇴 등으로 조회되지 않은 회원("탈퇴한 회원")이다.
 *
 * @param rating           별점. 목록의 숨긴 리뷰면 null
 * @param content          본문. 목록의 숨긴 리뷰면 null
 * @param authorProfileUrl 프로필 사진 주소. 아직 수집하지 않아 없으면 기본 아바타
 * @param mine             요청 회원이 쓴 리뷰인지
 * @param hidden           신고 누적·관리자로 숨겨졌는지
 */
public record PlaceReviewReadModel(
        Long id,
        Integer rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String authorNickname,
        String authorProfileUrl,
        boolean mine,
        boolean hidden) {}
