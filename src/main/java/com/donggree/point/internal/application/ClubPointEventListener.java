package com.donggree.point.internal.application;

import com.donggree.club.event.ClubRequestApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 동아리 요청 승인과 적립을 함께 커밋한다. 적립에 실패하면 승인도 롤백돼 관리자가 다시 승인할 수 있다. */
@Component
@RequiredArgsConstructor
public class ClubPointEventListener {

    private final PointRewardService rewards;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ClubRequestApprovedEvent event) {
        rewards.rewardClubApproval(
                event.memberId(),
                event.clubId(),
                event.firstRegistration(),
                event.recruitedGeneration(),
                event.updatedGeneration());
    }
}
