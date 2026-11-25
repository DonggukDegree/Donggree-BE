package com.donggree.point.internal.application;

import com.donggree.mystery.event.MysterySolvedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 정답과 보상을 함께 커밋한다. 적립 실패 시 정답 처리도 롤백해 같은 요청으로 재시도할 수 있다. */
@Component
@RequiredArgsConstructor
public class MysteryPointEventListener {
    private final PointRewardService rewards;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(MysterySolvedEvent event) {
        rewards.rewardMysterySolved(event.memberId(), event.gameDate());
    }
}
