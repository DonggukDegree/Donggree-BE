package com.donggree.place.internal.application.command;

/** 리뷰 작성·수정 명령. 별점 1~5, 본문 10~500자(앞뒤 공백 제외). */
public record WriteReviewCommand(int rating, String content) {}
