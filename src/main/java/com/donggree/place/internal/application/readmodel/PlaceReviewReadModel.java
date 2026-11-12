package com.donggree.place.internal.application.readmodel;

import java.time.LocalDateTime;

/**
 * 리뷰 한 건. 작성자 닉네임이 null이면 탈퇴 등으로 조회되지 않은 회원("탈퇴한 회원")이다.
 *
 * @param authorProfileUrl 프로필 사진 주소. 아직 수집하지 않아 없으면 기본 아바타
 * @param mine             요청 회원이 쓴 리뷰인지
 * @param hidden           신고 누적·관리자로 숨겨졌는지(본인 리뷰 조회에서만 true일 수 있음)
 */
public record PlaceReviewReadModel(
        Long id,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String authorNickname,
        String authorProfileUrl,
        boolean mine,
        boolean hidden) {}
