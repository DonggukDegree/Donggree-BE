package com.donggree.place.internal.presentation.dto;

import com.donggree.place.internal.application.readmodel.PlaceReviewReadModel;
import java.time.LocalDateTime;

/** 리뷰 한 건. 작성자 닉네임이 null이면 FE가 "탈퇴한 회원", 프로필 사진이 null이면 기본 아바타로 표시한다. */
public record ReviewResponse(
        Long id,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Author author,
        boolean mine,
        boolean hidden) {

    public static ReviewResponse from(PlaceReviewReadModel review) {
        return new ReviewResponse(
                review.id(),
                review.rating(),
                review.content(),
                review.createdAt(),
                review.updatedAt(),
                new Author(review.authorNickname(), review.authorProfileUrl()),
                review.mine(),
                review.hidden());
    }

    public record Author(String nickname, String profileUrl) {}
}
