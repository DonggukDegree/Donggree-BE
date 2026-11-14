package com.donggree.place.internal.application.event;

import com.donggree.place.internal.domain.enums.PlaceReportReason;

/**
 * 장소 신고 접수. 커밋 뒤 운영 알림(디스코드)으로 보낸다. 신고자 정보는 담지 않는다.
 *
 * @param pendingCount 접수 후 그 장소의 처리 대기 신고 수
 * @param autoHidden   이 신고로 장소가 자동 숨김됐는지
 */
public record PlaceReportedEvent(
        Long reportId,
        Long placeId,
        String placeName,
        PlaceReportReason reason,
        String description,
        long pendingCount,
        boolean autoHidden) {}
