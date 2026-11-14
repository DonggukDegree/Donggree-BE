package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.ReportPlaceCommand;
import com.donggree.place.internal.application.event.PlaceReportedEvent;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceReportCommandServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long PLACE_ID = 10L;

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceReportRepository reportRepository = mock(PlaceReportRepository.class);
    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final PlaceReportCommandService service =
            new PlaceReportCommandService(placeRepository, reportRepository, userLookupService, eventPublisher);
    private Place place;

    @BeforeEach
    void setUp() {
        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(true);
        place = PlaceFixtures.place(PLACE_ID, "동국반점", NEAR_CAMPUS);
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE)).willReturn(Optional.of(place));
        given(reportRepository.countByPlaceIdAndStatus(PLACE_ID, PlaceReportStatus.PENDING))
                .willReturn(1L);
        given(reportRepository.saveAndFlush(any(PlaceReport.class))).willAnswer(invocation -> {
            PlaceReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 50L);
            return report;
        });
    }

    @Test
    void 인증_회원은_등록자여도_장소를_신고하고_처리_대기로_접수된다() {
        Long reportId = service.report(MEMBER_ID, PLACE_ID, new ReportPlaceCommand(PlaceReportReason.CLOSED, null));

        assertThat(reportId).isEqualTo(50L);
        ArgumentCaptor<PlaceReport> saved = ArgumentCaptor.forClass(PlaceReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPlaceId()).isEqualTo(PLACE_ID);
        assertThat(saved.getValue().getReporterId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getValue().getStatus()).isEqualTo(PlaceReportStatus.PENDING);
        assertThat(place.isVisible()).isTrue();
        verify(eventPublisher)
                .publishEvent(new PlaceReportedEvent(50L, PLACE_ID, "동국반점", PlaceReportReason.CLOSED, null, 1L, false));
    }

    @Test
    void 서로_다른_회원의_처리_대기_신고가_3건이_되면_장소를_자동_숨긴다() {
        given(reportRepository.countByPlaceIdAndStatus(PLACE_ID, PlaceReportStatus.PENDING))
                .willReturn(2L, 3L);

        service.report(MEMBER_ID, PLACE_ID, closed());
        assertThat(place.isVisible()).isTrue();

        service.report(MEMBER_ID, PLACE_ID, new ReportPlaceCommand(PlaceReportReason.OTHER, "주차장 입구"));
        assertThat(place.isVisible()).isFalse();
        verify(eventPublisher)
                .publishEvent(
                        new PlaceReportedEvent(50L, PLACE_ID, "동국반점", PlaceReportReason.OTHER, "주차장 입구", 3L, true));
    }

    @Test
    void 미인증_회원은_403으로_막는다() {
        given(userLookupService.isStudentVerified(MEMBER_ID)).willReturn(false);

        assertError(() -> service.report(MEMBER_ID, PLACE_ID, closed()), PlaceErrorCode.VERIFICATION_REQUIRED);
    }

    @Test
    void 없거나_숨긴_장소는_404로_막는다() {
        given(placeRepository.findByIdAndStatus(PLACE_ID, PlaceStatus.VISIBLE)).willReturn(Optional.empty());

        assertError(() -> service.report(MEMBER_ID, PLACE_ID, closed()), PlaceErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    void 같은_장소에_처리_대기_신고가_있으면_409_3으로_막는다() {
        given(reportRepository.existsByPlaceIdAndReporterIdAndStatus(PLACE_ID, MEMBER_ID, PlaceReportStatus.PENDING))
                .willReturn(true);

        assertError(() -> service.report(MEMBER_ID, PLACE_ID, closed()), PlaceErrorCode.DUPLICATE_REPORT);
    }

    @Test
    void 동시에_제출해_유일_인덱스에_걸리면_409_3으로_응답한다() {
        given(reportRepository.saveAndFlush(any(PlaceReport.class)))
                .willThrow(new DataIntegrityViolationException("uk_place_report_pending"));

        assertThatThrownBy(() -> service.report(MEMBER_ID, PLACE_ID, closed()))
                .isInstanceOf(GeneralException.class)
                .satisfies(
                        e -> assertThat(((GeneralException) e).getCode()).isEqualTo(PlaceErrorCode.DUPLICATE_REPORT));
    }

    @Test
    void 기타_사유에_설명이_없으면_400_4로_막는다() {
        assertError(
                () -> service.report(MEMBER_ID, PLACE_ID, new ReportPlaceCommand(PlaceReportReason.OTHER, " ")),
                PlaceErrorCode.INVALID_REPORT);
    }

    private static ReportPlaceCommand closed() {
        return new ReportPlaceCommand(PlaceReportReason.CLOSED, null);
    }

    private void assertError(ThrowingCallable call, PlaceErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode()).isEqualTo(code));
        verify(reportRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
