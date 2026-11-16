package com.donggree.point.internal.application.readmodel;

import com.donggree.point.internal.domain.PointReason;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 적립 내역 한 페이지(최신순). 다음 페이지는 nextCursor를 cursor로 넘겨 받는다.
 *
 * @param nextCursor 다음 페이지 커서. 마지막 페이지면 null
 */
public record PointHistoryPageReadModel(List<Item> histories, Long nextCursor) {

    /** @param revoked 대상이 숨겨져 회수된 적립. 합계에서 빠진다. */
    public record Item(Long historyId, PointReason reason, int amount, boolean revoked, LocalDateTime createdAt) {}
}
