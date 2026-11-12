package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.ReportPlaceCommand;
import com.donggree.place.internal.application.event.PlaceReportedEvent;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동글맵 장소 신고 응용 서비스. 등록자를 포함한 인증 회원 누구나 신고한다.
 * 서로 다른 회원의 처리 대기 신고가 3건 쌓이면 장소를 자동으로 숨기고, 그 밖의 처리는 관리자가 한다.
 * 접수가 커밋되면 운영 알림 이벤트를 발행한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceReportCommandService {

    private final PlaceRepository placeRepository;
    private final PlaceReportRepository reportRepository;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 신고를 접수하고 ID를 반환한다. 같은 장소에 처리 대기 신고가 있으면 409로 막는다.
     * 동시에 두 번 제출해도 DB 부분 유일 인덱스(장소·신고자·대기)가 한 건만 남긴다.
     */
    @Transactional
    public Long report(Long memberId, Long placeId, ReportPlaceCommand command) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED);
        }
        Place place = placeRepository
                .findByIdAndStatus(placeId, PlaceStatus.VISIBLE)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
        if (reportRepository.existsByPlaceIdAndReporterIdAndStatus(placeId, memberId, PlaceReportStatus.PENDING)) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REPORT);
        }
        PlaceReport report;
        try {
            report = PlaceReport.submit(placeId, memberId, command.reason(), command.description());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_REPORT);
        }
        Long reportId;
        try {
            reportId = reportRepository.saveAndFlush(report).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REPORT);
        }
        long pendingCount = reportRepository.countByPlaceIdAndStatus(placeId, PlaceReportStatus.PENDING);
        boolean autoHidden = pendingCount >= PlaceReport.AUTO_HIDE_PENDING_COUNT;
        if (autoHidden) {
            place.hide();
        }
        eventPublisher.publishEvent(new PlaceReportedEvent(
                reportId,
                placeId,
                place.getName(),
                report.getReason(),
                report.getDescription(),
                pendingCount,
                autoHidden));
        return reportId;
    }
}
