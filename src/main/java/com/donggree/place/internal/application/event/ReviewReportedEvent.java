package com.donggree.place.internal.application.event;

import com.donggree.place.internal.domain.enums.ReviewReportReason;

/**
 * 리뷰 신고 접수. 커밋 뒤 운영 알림(디스코드)으로 보낸다. 신고자·작성자 정보는 담지 않는다.
 *
 * @param reviewExcerpt 신고된 리뷰 본문 앞부분
 * @param pendingCount  접수 후 그 리뷰의 처리 대기 신고 수
 * @param autoHidden    이 신고로 리뷰가 자동 숨김됐는지
 */
public record ReviewReportedEvent(
        Long reportId,
        Long reviewId,
        Long placeId,
        String placeName,
        int rating,
        String reviewExcerpt,
        ReviewReportReason reason,
        String description,
        long pendingCount,
        boolean autoHidden) {}
