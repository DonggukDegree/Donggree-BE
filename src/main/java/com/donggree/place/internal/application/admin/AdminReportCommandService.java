package com.donggree.place.internal.application.admin;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReport;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.user.MemberSuspensionRecord;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspendMemberCommand;
import com.donggree.user.SuspensionSource;
import com.donggree.user.UserModerationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 신고 처리. 신고 한 건을 처리 완료·기각하고, 신고를 근거로 회원을 정지한다(정지하면 그 신고는 처리 완료).
 * 장소·리뷰 숨김과 수정 되돌리기는 각 관리 서비스가 맡는다.
 */
@Service
@RequiredArgsConstructor
public class AdminReportCommandService {

    private final PlaceReportRepository placeReportRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final PlaceReviewRepository reviewRepository;
    private final UserModerationService moderationService;

    @Transactional
    public void resolvePlaceReport(Long reportId) {
        process(() -> placeReport(reportId).resolve(LocalDateTime.now()));
    }

    @Transactional
    public void rejectPlaceReport(Long reportId) {
        process(() -> placeReport(reportId).reject(LocalDateTime.now()));
    }

    @Transactional
    public void resolveReviewReport(Long reportId) {
        process(() -> reviewReport(reportId).resolve(LocalDateTime.now()));
    }

    @Transactional
    public void rejectReviewReport(Long reportId) {
        process(() -> reviewReport(reportId).reject(LocalDateTime.now()));
    }

    /**
     * 장소 신고를 근거로 회원(등록자·수정자 중 관리자가 고른 회원)을 정지하고, 신고가 대기 중이면 처리 완료한다.
     */
    @Transactional
    public MemberSuspensionRecord suspendByPlaceReport(
            Long reportId, Long memberId, int days, String note, Long adminId) {
        PlaceReport report = placeReport(reportId);
        if (memberId == null) {
            throw new GeneralException(PlaceErrorCode.SUSPENSION_TARGET_MISSING);
        }
        MemberSuspensionRecord record = moderationService.suspend(new SuspendMemberCommand(
                memberId, days, SuspensionSource.REPORT, ReportTargetType.PLACE, reportId, note, adminId));
        if (report.isPending()) {
            report.resolve(LocalDateTime.now());
        }
        return record;
    }

    /** 리뷰 신고를 근거로 리뷰 작성자를 정지하고, 신고가 대기 중이면 처리 완료한다. */
    @Transactional
    public MemberSuspensionRecord suspendByReviewReport(Long reportId, int days, String note, Long adminId) {
        ReviewReport report = reviewReport(reportId);
        Long authorId = reviewRepository
                .findById(report.getReviewId())
                .map(PlaceReview::getAuthorId)
                .orElse(null);
        if (authorId == null) {
            throw new GeneralException(PlaceErrorCode.SUSPENSION_TARGET_MISSING);
        }
        MemberSuspensionRecord record = moderationService.suspend(new SuspendMemberCommand(
                authorId, days, SuspensionSource.REPORT, ReportTargetType.REVIEW, reportId, note, adminId));
        if (report.isPending()) {
            report.resolve(LocalDateTime.now());
        }
        return record;
    }

    private PlaceReport placeReport(Long reportId) {
        return placeReportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REPORT_NOT_FOUND));
    }

    private ReviewReport reviewReport(Long reportId) {
        return reviewReportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REPORT_NOT_FOUND));
    }

    private static void process(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException exception) {
            throw new GeneralException(PlaceErrorCode.REPORT_ALREADY_PROCESSED);
        }
    }
}
