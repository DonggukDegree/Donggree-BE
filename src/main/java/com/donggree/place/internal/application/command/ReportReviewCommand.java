package com.donggree.place.internal.application.command;

import com.donggree.place.internal.domain.enums.ReviewReportReason;

/** 리뷰 신고 명령. 기타 사유는 설명이 필요하다. */
public record ReportReviewCommand(ReviewReportReason reason, String description) {}
