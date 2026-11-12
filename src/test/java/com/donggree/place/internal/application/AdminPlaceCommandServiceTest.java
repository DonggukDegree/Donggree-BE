package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminPlaceCommandServiceTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceReportRepository reportRepository = mock(PlaceReportRepository.class);
    private final PlaceEditHistoryRepository editHistoryRepository = mock(PlaceEditHistoryRepository.class);
    private final AdminPlaceCommandService service =
            new AdminPlaceCommandService(placeRepository, reportRepository, editHistoryRepository);

    @Test
    void 숨기면_처리_대기_신고를_처리_완료로_바꾼다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        given(placeRepository.findById(10L)).willReturn(Optional.of(place));

        service.hide(10L);

        assertThat(place.isVisible()).isFalse();
        verify(reportRepository).resolvePending(eq(10L), any());
    }

    @Test
    void 숨김을_해제하면_처리_대기_신고를_기각해_자동_숨김_집계를_다시_시작한다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        place.hide();
        given(placeRepository.findById(10L)).willReturn(Optional.of(place));

        service.show(10L);

        assertThat(place.isVisible()).isTrue();
        verify(reportRepository).rejectPending(eq(10L), any());
        verify(reportRepository, never()).resolvePending(any(), any());
    }

    @Test
    void 없는_장소는_404다() {
        given(placeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.hide(99L))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND));
        assertThatThrownBy(() -> service.show(99L)).isInstanceOf(GeneralException.class);
    }

    @Test
    void 수정_이력의_수정_전_값으로_되돌리고_되돌린_것도_관리자_수정으로_남긴다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        given(placeRepository.findById(10L)).willReturn(Optional.of(place));
        var vandalized = place.editInfo(
                        7L, new com.donggree.place.internal.domain.PlaceInfo("광고 문구", "광고 문구입니다", java.util.List.of()))
                .orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(vandalized, "id", 5L);
        given(editHistoryRepository.findByIdAndPlaceId(5L, 10L)).willReturn(Optional.of(vandalized));

        service.revertEdit(10L, 5L, 1L);

        assertThat(place.getBusinessHours()).isEqualTo("평일 11:00-21:00");
        assertThat(place.getMenus()).hasSize(2);
        org.mockito.ArgumentCaptor<com.donggree.place.internal.domain.PlaceEditHistory> saved =
                org.mockito.ArgumentCaptor.forClass(com.donggree.place.internal.domain.PlaceEditHistory.class);
        verify(editHistoryRepository).save(saved.capture());
        assertThat(saved.getValue().getEditorId()).isEqualTo(1L);

        given(editHistoryRepository.findByIdAndPlaceId(6L, 10L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.revertEdit(10L, 6L, 1L)).isInstanceOf(GeneralException.class);
    }
}
