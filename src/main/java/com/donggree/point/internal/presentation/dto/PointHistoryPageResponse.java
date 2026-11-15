package com.donggree.point.internal.presentation.dto;

import com.donggree.point.internal.application.readmodel.PointHistoryPageReadModel;
import com.donggree.point.internal.domain.PointReason;
import java.time.LocalDateTime;
import java.util.List;

/** 적립 내역 한 페이지(최신순). 다음 페이지는 nextCursor를 cursor로 넘긴다(마지막이면 null). */
public record PointHistoryPageResponse(List<History> histories, Long nextCursor) {

    public static PointHistoryPageResponse from(PointHistoryPageReadModel page) {
        return new PointHistoryPageResponse(
                page.histories().stream()
                        .map(item -> new History(
                                item.historyId(), item.reason(), item.amount(), item.revoked(), item.createdAt()))
                        .toList(),
                page.nextCursor());
    }

    public record History(Long historyId, PointReason reason, int amount, boolean revoked, LocalDateTime createdAt) {}
}
