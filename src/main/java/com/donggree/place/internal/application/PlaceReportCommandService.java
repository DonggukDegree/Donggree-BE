package com.donggree.place.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.command.ReportPlaceCommand;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.user.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동글맵 장소 신고 응용 서비스. 등록자를 포함한 인증 회원 누구나 신고한다.
 * 신고만으로 장소를 숨기지 않으며, 관리자가 확인한 뒤 숨김·수정한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceReportCommandService {

    private final PlaceRepository placeRepository;
    private final PlaceReportRepository reportRepository;
    private final UserLookupService userLookupService;

    /**
     * 신고를 접수하고 ID를 반환한다. 같은 장소에 처리 대기 신고가 있으면 409로 막는다.
     * 동시에 두 번 제출해도 DB 부분 유일 인덱스(장소·신고자·대기)가 한 건만 남긴다.
     */
    @Transactional
    public Long report(Long memberId, Long placeId, ReportPlaceCommand command) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(PlaceErrorCode.VERIFICATION_REQUIRED);
        }
        if (placeRepository.findByIdAndStatus(placeId, PlaceStatus.VISIBLE).isEmpty()) {
            throw new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND);
        }
        if (reportRepository.existsByPlaceIdAndReporterIdAndStatus(placeId, memberId, PlaceReportStatus.PENDING)) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REPORT);
        }
        PlaceReport report;
        try {
            report = PlaceReport.submit(placeId, memberId, command.reason(), command.description());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorCode.INVALID_REPORT);
        }
        try {
            return reportRepository.saveAndFlush(report).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(PlaceErrorCode.DUPLICATE_REPORT);
        }
    }
}
