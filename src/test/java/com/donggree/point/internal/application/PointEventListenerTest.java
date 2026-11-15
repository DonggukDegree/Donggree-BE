package com.donggree.point.internal.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.donggree.graduation.event.GraduationReportViewedEvent;
import com.donggree.place.event.PlaceInfoEditedEvent;
import com.donggree.place.event.PlaceRegisteredEvent;
import com.donggree.place.event.PlaceReviewVisibilityChangedEvent;
import com.donggree.place.event.PlaceReviewWrittenEvent;
import com.donggree.place.event.PlaceVisibilityChangedEvent;
import com.donggree.user.event.MemberWithdrawnEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class PointEventListenerTest {

    private final PointRewardService rewardService = mock(PointRewardService.class);
    private final PointEventListener listener =
            new PointEventListener(rewardService, mock(PlatformTransactionManager.class));

    @Test
    void 활동_이벤트를_적립_회수로_넘긴다() {
        listener.on(new GraduationReportViewedEvent(1L));
        listener.on(new PlaceRegisteredEvent(10L, 1L));
        listener.on(new PlaceReviewWrittenEvent(30L, 10L, 1L, true));
        listener.on(new PlaceInfoEditedEvent(10L, 2L));
        listener.on(new PlaceVisibilityChangedEvent(10L, false));
        listener.on(new PlaceReviewVisibilityChangedEvent(30L, 10L, 1L, true));
        listener.on(new MemberWithdrawnEvent(1L));

        verify(rewardService).rewardReportFirstView(1L);
        verify(rewardService).rewardPlaceRegister(1L, 10L);
        verify(rewardService).rewardReview(1L, 10L, true);
        verify(rewardService).rewardInfoEdit(2L, 10L);
        verify(rewardService).onPlaceVisibilityChanged(10L, false);
        verify(rewardService).onReviewVisibilityChanged(1L, 10L, true);
        verify(rewardService).deleteAllOfMember(1L);
    }

    @Test
    void 동기_적립이_실패해도_원래_요청을_막지_않는다() {
        willThrow(new IllegalStateException("db down")).given(rewardService).rewardReportFirstView(1L);
        willThrow(new IllegalStateException("db down")).given(rewardService).rewardPlaceRegister(1L, 10L);
        willThrow(new IllegalStateException("db down")).given(rewardService).rewardReview(1L, 10L, true);
        willThrow(new IllegalStateException("db down")).given(rewardService).rewardInfoEdit(1L, 10L);

        assertThatCode(() -> listener.on(new GraduationReportViewedEvent(1L))).doesNotThrowAnyException();
        assertThatCode(() -> listener.on(new PlaceRegisteredEvent(10L, 1L))).doesNotThrowAnyException();
        assertThatCode(() -> listener.on(new PlaceReviewWrittenEvent(30L, 10L, 1L, true)))
                .doesNotThrowAnyException();
        assertThatCode(() -> listener.on(new PlaceInfoEditedEvent(10L, 1L))).doesNotThrowAnyException();
    }
}
