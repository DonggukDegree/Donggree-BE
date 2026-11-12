package com.donggree.place.internal.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.user.event.MemberWithdrawnEvent;
import org.junit.jupiter.api.Test;

class PlaceMemberWithdrawnEventListenerTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceEditHistoryRepository editHistoryRepository = mock(PlaceEditHistoryRepository.class);
    private final PlaceReportRepository reportRepository = mock(PlaceReportRepository.class);
    private final PlaceReviewRepository reviewRepository = mock(PlaceReviewRepository.class);
    private final ReviewReportRepository reviewReportRepository = mock(ReviewReportRepository.class);
    private final PlaceMemberWithdrawnEventListener listener = new PlaceMemberWithdrawnEventListener(
            placeRepository, editHistoryRepository, reportRepository, reviewRepository, reviewReportRepository);

    @Test
    void 탈퇴_회원의_등록자와_수정자와_신고자와_리뷰_작성자_연결을_끊는다() {
        listener.on(new MemberWithdrawnEvent(7L));

        verify(placeRepository).clearCreatedBy(7L);
        verify(editHistoryRepository).clearEditor(7L);
        verify(reportRepository).clearReporter(7L);
        verify(reviewRepository).clearAuthor(7L);
        verify(reviewReportRepository).clearReporter(7L);
    }
}
