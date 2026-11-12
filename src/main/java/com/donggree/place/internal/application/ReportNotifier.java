package com.donggree.place.internal.application;

import com.donggree.place.internal.application.event.PlaceReportedEvent;
import com.donggree.place.internal.application.event.ReviewReportedEvent;

/** 신고 접수 운영 알림 포트. 알림 실패가 신고 접수를 되돌리지 않도록 구현체는 예외를 밖으로 던지지 않는다. */
public interface ReportNotifier {

    void notifyPlaceReported(PlaceReportedEvent event);

    void notifyReviewReported(ReviewReportedEvent event);
}
