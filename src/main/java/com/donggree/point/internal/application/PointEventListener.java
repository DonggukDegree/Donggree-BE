package com.donggree.point.internal.application;

import com.donggree.graduation.event.GraduationReportViewedEvent;
import com.donggree.place.event.PlaceInfoEditedEvent;
import com.donggree.place.event.PlaceRegisteredEvent;
import com.donggree.place.event.PlaceReviewVisibilityChangedEvent;
import com.donggree.place.event.PlaceReviewWrittenEvent;
import com.donggree.place.event.PlaceVisibilityChangedEvent;
import com.donggree.point.internal.domain.PointReason;
import com.donggree.user.event.MemberWithdrawnEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 다른 모듈의 활동 이벤트를 포인트 적립·회수로 바꾼다.
 *
 * <p>장소 등록·리뷰 작성·정보 수정 적립은 원래 트랜잭션이 커밋된 직후 같은 요청 안에서(동기) 새 트랜잭션으로 처리한다.
 * 응답이 나가기 전에 적립이 끝나 있어야 화면이 바로 "적립됐어요"를 안내할 수 있기 때문이다.
 * 실패해도 이미 커밋된 원래 요청을 실패로 바꾸지 않도록 로그만 남긴다.
 * 숨김에 따른 회수·복구와 탈퇴 정리는 기다릴 이유가 없어 비동기로 처리한다.
 */
@Slf4j
@Component
public class PointEventListener {

    private final PointRewardService rewardService;
    private final TransactionTemplate newTransaction;

    public PointEventListener(PointRewardService rewardService, PlatformTransactionManager transactionManager) {
        this.rewardService = rewardService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 리포트 조회는 트랜잭션 없이 동기로 발행되므로 여기서 실패를 삼켜 조회 응답을 지킨다. */
    @EventListener
    void on(GraduationReportViewedEvent event) {
        rewardSafely(
                PointReason.GRADUATION_REPORT_FIRST_VIEW, () -> rewardService.rewardReportFirstView(event.memberId()));
    }

    @TransactionalEventListener
    void on(PlaceRegisteredEvent event) {
        rewardSafely(
                PointReason.PLACE_REGISTER, () -> rewardService.rewardPlaceRegister(event.memberId(), event.placeId()));
    }

    @TransactionalEventListener
    void on(PlaceReviewWrittenEvent event) {
        rewardSafely(
                PointReason.PLACE_REVIEW,
                () -> rewardService.rewardReview(event.memberId(), event.placeId(), event.firstReviewOfPlace()));
    }

    @TransactionalEventListener
    void on(PlaceInfoEditedEvent event) {
        rewardSafely(
                PointReason.PLACE_INFO_EDIT, () -> rewardService.rewardInfoEdit(event.memberId(), event.placeId()));
    }

    @ApplicationModuleListener
    void on(PlaceVisibilityChangedEvent event) {
        rewardService.onPlaceVisibilityChanged(event.placeId(), event.visible());
    }

    @ApplicationModuleListener
    void on(PlaceReviewVisibilityChangedEvent event) {
        rewardService.onReviewVisibilityChanged(event.authorId(), event.placeId(), event.visible());
    }

    @ApplicationModuleListener
    void on(MemberWithdrawnEvent event) {
        rewardService.deleteAllOfMember(event.memberId());
    }

    private void rewardSafely(PointReason reason, Runnable reward) {
        try {
            newTransaction.executeWithoutResult(status -> reward.run());
        } catch (RuntimeException exception) {
            log.warn(
                    "포인트 적립 실패 event=\"point_reward_failure\" reason=\"{}\" exception_type=\"{}\"",
                    reason,
                    exception.getClass().getSimpleName());
        }
    }
}
