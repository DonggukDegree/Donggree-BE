package com.donggree.place.internal.application.admin;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.admin.AdminReportModels.EditHistoryItem;
import com.donggree.place.internal.application.admin.AdminReportModels.InfoSnapshot;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceReportItem;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceSnapshot;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewReportItem;
import com.donggree.place.internal.application.exception.PlaceErrorCode;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceEditHistory;
import com.donggree.place.internal.domain.PlaceEditHistoryRepository;
import com.donggree.place.internal.domain.PlaceInfo;
import com.donggree.place.internal.domain.PlaceReport;
import com.donggree.place.internal.domain.PlaceReportRepository;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.PlaceReviewRepository;
import com.donggree.place.internal.domain.ReviewReport;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 통합 신고 대시보드 조회. 장소·리뷰 신고를 상태별 최신순으로 20개씩 보여주고, 상세에서 같은 대상의 신고 전체와
 * 수정 이력·작성자를 함께 본다. 신고자·작성자·수정자는 이름·학번·단과대·학과가 붙은 회원 행으로 내려간다.
 */
@Service
@RequiredArgsConstructor
public class AdminReportQueryService {

    public static final int PAGE_SIZE = 20;

    private final PlaceReportRepository placeReportRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository reviewRepository;
    private final PlaceEditHistoryRepository editHistoryRepository;
    private final AdminMemberReader memberReader;

    /** @param status null이면 전체 */
    @Transactional(readOnly = true)
    public Page<PlaceReportItem> getPlaceReports(PlaceReportStatus status, int page) {
        var result = placeReportRepository.findForAdmin(status, pageRequest(page));
        return new Page<>(
                toPlaceItems(result.getContent()), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public PlaceReportDetail getPlaceReport(Long reportId) {
        PlaceReport report = placeReportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REPORT_NOT_FOUND));
        Place place = placeRepository
                .findById(report.getPlaceId())
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
        List<PlaceReport> placeReports = placeReportRepository.findByPlaceIdOrderByIdAsc(place.getId());
        List<PlaceEditHistory> histories = editHistoryRepository.findTop20ByPlaceIdOrderByIdDesc(place.getId());

        List<Long> memberIds = new ArrayList<>();
        memberIds.add(place.getCreatedBy());
        histories.forEach(history -> memberIds.add(history.getEditorId()));
        Map<Long, AdminMemberSummary> members = memberReader.read(memberIds);

        List<PlaceReportItem> items = toPlaceItems(placeReports);
        PlaceReportItem target = items.stream()
                .filter(item -> item.reportId().equals(reportId))
                .findFirst()
                .orElseThrow();
        PlaceInfo info = place.info();
        return new PlaceReportDetail(
                target,
                new PlaceSnapshot(
                        place.getId(),
                        place.getName(),
                        place.getCategory(),
                        place.getSubCategory(),
                        place.getStatus(),
                        info.businessHours(),
                        info.description(),
                        info.menus(),
                        info.tags(),
                        place.getCreatedAt(),
                        place.getInfoUpdatedAt()),
                members.get(place.getCreatedBy()),
                histories.stream()
                        .map(history -> new EditHistoryItem(
                                history.getId(),
                                members.get(history.getEditorId()),
                                history.getEditedAt(),
                                snapshot(history.before()),
                                snapshot(history.after())))
                        .toList(),
                items);
    }

    @Transactional(readOnly = true)
    public Page<ReviewReportItem> getReviewReports(PlaceReportStatus status, int page) {
        var result = reviewReportRepository.findForAdmin(status, pageRequest(page));
        return new Page<>(
                toReviewItems(result.getContent()), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public ReviewReportDetail getReviewReport(Long reportId) {
        ReviewReport report = reviewReportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.REPORT_NOT_FOUND));
        List<ReviewReportItem> items =
                toReviewItems(reviewReportRepository.findByReviewIdOrderByIdAsc(report.getReviewId()));
        ReviewReportItem target = items.stream()
                .filter(item -> item.reportId().equals(reportId))
                .findFirst()
                .orElseThrow();
        return new ReviewReportDetail(target, items);
    }

    private List<PlaceReportItem> toPlaceItems(List<PlaceReport> reports) {
        Map<Long, Place> places =
                placeRepository
                        .findAllById(reports.stream()
                                .map(PlaceReport::getPlaceId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(Place::getId, Function.identity()));
        Map<Long, AdminMemberSummary> reporters = memberReader.read(
                reports.stream().map(PlaceReport::getReporterId).toList());
        Map<Long, Long> pendingCounts = new HashMap<>();
        return reports.stream()
                .map(report -> {
                    Place place = places.get(report.getPlaceId());
                    long pending = pendingCounts.computeIfAbsent(
                            report.getPlaceId(),
                            placeId ->
                                    placeReportRepository.countByPlaceIdAndStatus(placeId, PlaceReportStatus.PENDING));
                    return new PlaceReportItem(
                            report.getId(),
                            report.getReason(),
                            report.getDescription(),
                            report.getStatus(),
                            report.getCreatedAt(),
                            report.getProcessedAt(),
                            reporters.get(report.getReporterId()),
                            report.getPlaceId(),
                            place == null ? null : place.getName(),
                            place == null ? null : place.getCategory(),
                            place == null ? null : place.getStatus(),
                            pending);
                })
                .toList();
    }

    private List<ReviewReportItem> toReviewItems(List<ReviewReport> reports) {
        Map<Long, PlaceReview> reviews =
                reviewRepository
                        .findAllById(reports.stream()
                                .map(ReviewReport::getReviewId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(PlaceReview::getId, Function.identity()));
        Map<Long, Place> places =
                placeRepository
                        .findAllById(reviews.values().stream()
                                .map(PlaceReview::getPlaceId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(Place::getId, Function.identity()));
        List<Long> memberIds = new ArrayList<>(
                reports.stream().map(ReviewReport::getReporterId).toList());
        reviews.values().forEach(review -> memberIds.add(review.getAuthorId()));
        Map<Long, AdminMemberSummary> members = memberReader.read(memberIds);
        Map<Long, Long> pendingCounts = new HashMap<>();
        return reports.stream()
                .map(report -> {
                    PlaceReview review = reviews.get(report.getReviewId());
                    Place place = review == null ? null : places.get(review.getPlaceId());
                    long pending = pendingCounts.computeIfAbsent(
                            report.getReviewId(),
                            reviewId -> reviewReportRepository.countByReviewIdAndStatus(
                                    reviewId, PlaceReportStatus.PENDING));
                    return new ReviewReportItem(
                            report.getId(),
                            report.getReason(),
                            report.getDescription(),
                            report.getStatus(),
                            report.getCreatedAt(),
                            report.getProcessedAt(),
                            members.get(report.getReporterId()),
                            report.getReviewId(),
                            review == null ? 0 : review.getRating(),
                            review == null ? null : review.getContent(),
                            review == null ? null : review.getStatus(),
                            review != null && review.isDeleted(),
                            review == null ? null : members.get(review.getAuthorId()),
                            place == null ? null : place.getId(),
                            place == null ? null : place.getName(),
                            pending);
                })
                .toList();
    }

    private static InfoSnapshot snapshot(PlaceInfo info) {
        return new InfoSnapshot(info.businessHours(), info.description(), info.menus(), info.tags());
    }

    private static PageRequest pageRequest(int page) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
    }
}
