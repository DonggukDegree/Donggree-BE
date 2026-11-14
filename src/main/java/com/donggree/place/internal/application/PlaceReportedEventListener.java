package com.donggree.place.internal.application;

import com.donggree.place.internal.application.event.PlaceReportedEvent;
import com.donggree.place.internal.application.event.ReviewReportedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** 신고 접수가 커밋된 뒤 비동기로 운영 알림을 보낸다. 신고 요청 응답 시간에 외부 호출이 끼지 않는다. */
@Component
@RequiredArgsConstructor
public class PlaceReportedEventListener {

    private final ReportNotifier reportNotifier;

    @ApplicationModuleListener
    void on(PlaceReportedEvent event) {
        reportNotifier.notifyPlaceReported(event);
    }

    @ApplicationModuleListener
    void on(ReviewReportedEvent event) {
        reportNotifier.notifyReviewReported(event);
    }
}
