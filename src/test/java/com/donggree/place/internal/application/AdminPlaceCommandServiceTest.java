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
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminPlaceCommandServiceTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceReportRepository reportRepository = mock(PlaceReportRepository.class);
    private final AdminPlaceCommandService service = new AdminPlaceCommandService(placeRepository, reportRepository);

    @Test
    void 숨기면_처리_대기_신고를_처리_완료로_바꾼다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        given(placeRepository.findById(10L)).willReturn(Optional.of(place));

        service.hide(10L);

        assertThat(place.isVisible()).isFalse();
        verify(reportRepository).resolvePending(eq(10L), any());
    }

    @Test
    void 숨김을_해제해도_신고_상태는_그대로_둔다() {
        Place place = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        place.hide();
        given(placeRepository.findById(10L)).willReturn(Optional.of(place));

        service.show(10L);

        assertThat(place.isVisible()).isTrue();
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
}
