package com.donggree.point.internal.application;

import com.donggree.point.internal.application.readmodel.PointHistoryPageReadModel;
import com.donggree.point.internal.domain.PointHistory;
import com.donggree.point.internal.domain.PointHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointQueryService {

    static final int PAGE_SIZE = 20;

    private final PointHistoryRepository historyRepository;

    /** 회수되지 않은 적립의 합계. */
    public long getTotalPoints(Long memberId) {
        return historyRepository.sumActiveAmount(memberId);
    }

    /**
     * 내 적립 내역을 최신순으로 {@value #PAGE_SIZE}개씩 반환한다. 회수된 적립도 표시용으로 포함한다.
     *
     * @param cursor 이전 페이지의 nextCursor. 첫 페이지는 null
     */
    public PointHistoryPageReadModel getHistories(Long memberId, Long cursor) {
        Limit limit = Limit.of(PAGE_SIZE + 1);
        List<PointHistory> fetched = cursor == null
                ? historyRepository.findByMemberIdOrderByIdDesc(memberId, limit)
                : historyRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, cursor, limit);
        boolean hasNext = fetched.size() > PAGE_SIZE;
        List<PointHistory> page = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;
        List<PointHistoryPageReadModel.Item> items = page.stream()
                .map(history -> new PointHistoryPageReadModel.Item(
                        history.getId(),
                        history.getReason(),
                        history.getAmount(),
                        history.isRevoked(),
                        history.getCreatedAt()))
                .toList();
        return new PointHistoryPageReadModel(items, hasNext ? page.getLast().getId() : null);
    }
}
