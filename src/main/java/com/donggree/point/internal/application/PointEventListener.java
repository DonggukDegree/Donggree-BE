package com.donggree.point.internal.application;

import com.donggree.graduation.event.GraduationReportViewedEvent;
import com.donggree.place.event.PlaceInfoEditedEvent;
import com.donggree.place.event.PlaceRegisteredEvent;
import com.donggree.place.event.PlaceReviewVisibilityChangedEvent;
import com.donggree.place.event.PlaceReviewWrittenEvent;
import com.donggree.place.event.PlaceVisibilityChangedEvent;
import com.donggree.user.event.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 다른 모듈의 활동 이벤트를 포인트 적립·회수로 바꾼다.
 * 장소·리뷰·탈퇴 이벤트는 원래 트랜잭션이 커밋된 뒤 비동기로 처리해, 포인트 실패가 원래 요청을 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final PointRewardService rewardService;

    /** 리포트 조회는 트랜잭션 없이 동기로 발행되므로 여기서 실패를 삼켜 조회 응답을 지킨다. */
    @EventListener
    void on(GraduationReportViewedEvent event) {
        try {
            rewardService.rewardReportFirstView(event.memberId());
        } catch (RuntimeException exception) {
            log.warn(
                    "포인트 적립 실패 event=\"point_reward_failure\" reason=\"GRADUATION_REPORT_FIRST_VIEW\""
                            + " exception_type=\"{}\"",
                    exception.getClass().getSimpleName());
        }
    }

    @ApplicationModuleListener
    void on(PlaceRegisteredEvent event) {
        rewardService.rewardPlaceRegister(event.memberId(), event.placeId());
    }

    @ApplicationModuleListener
    void on(PlaceReviewWrittenEvent event) {
        rewardService.rewardReview(event.memberId(), event.placeId(), event.firstReviewOfPlace());
    }

    @ApplicationModuleListener
    void on(PlaceInfoEditedEvent event) {
        rewardService.rewardInfoEdit(event.memberId(), event.placeId());
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
}
