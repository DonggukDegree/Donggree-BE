package com.donggree.place.internal.application.admin;

import com.donggree.place.internal.application.PlaceQueries;
import com.donggree.place.internal.application.PlaceQueries.AdminReviewState;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.application.admin.AdminReportModels.PlaceRow;
import com.donggree.place.internal.application.admin.AdminReportModels.ReviewRow;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceRepository;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 신고 관리 화면 하단의 전체 장소·리뷰 목록. 신고 여부와 관계없이 상태·처리 대기 신고 수를 함께 보여주고,
 * 행에서 바로 숨김·해제하거나 등록자·작성자 회원 상세로 넘어간다. 최신순 20개씩.
 */
@Service
@RequiredArgsConstructor
public class AdminContentQueryService {

    public static final int PAGE_SIZE = 20;

    private final PlaceQueries placeQueries;
    private final PlaceRepository placeRepository;
    private final AdminMemberReader memberReader;

    /**
     * @param status  null이면 전체
     * @param keyword 장소 이름 부분 일치. 비었으면 전체
     */
    @Transactional(readOnly = true)
    public Page<PlaceRow> getPlaces(PlaceStatus status, String keyword, int page) {
        var result = placeQueries.findPlacesForAdmin(status, normalize(keyword), pageRequest(page));
        List<Place> places = result.getContent();
        Map<Long, Long> pending = placeQueries.countPendingPlaceReports(
                places.stream().map(Place::getId).toList());
        Map<Long, AdminMemberSummary> registrants =
                memberReader.read(places.stream().map(Place::getCreatedBy).toList());
        return new Page<>(
                places.stream()
                        .map(place -> new PlaceRow(
                                place.getId(),
                                place.getName(),
                                place.getCategory(),
                                place.getSubCategory(),
                                place.getStatus(),
                                pending.getOrDefault(place.getId(), 0L),
                                place.getCreatedBy() == null ? null : registrants.get(place.getCreatedBy()),
                                place.getCreatedAt()))
                        .toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
    }

    /**
     * @param state   null이면 전체
     * @param keyword 리뷰 본문 또는 장소 이름 부분 일치. 비었으면 전체
     */
    @Transactional(readOnly = true)
    public Page<ReviewRow> getReviews(AdminReviewState state, String keyword, int page) {
        var result = placeQueries.findReviewsForAdmin(state, normalize(keyword), pageRequest(page));
        List<PlaceReview> reviews = result.getContent();
        Map<Long, Long> pending = placeQueries.countPendingReviewReports(
                reviews.stream().map(PlaceReview::getId).toList());
        Map<Long, String> placeNames =
                placeRepository
                        .findAllById(reviews.stream()
                                .map(PlaceReview::getPlaceId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(Place::getId, Place::getName));
        Map<Long, AdminMemberSummary> authors = memberReader.read(
                new ArrayList<>(reviews.stream().map(PlaceReview::getAuthorId).toList()));
        return new Page<>(
                reviews.stream()
                        .map(review -> new ReviewRow(
                                review.getId(),
                                review.getPlaceId(),
                                placeNames.get(review.getPlaceId()),
                                review.getRating(),
                                review.getContent(),
                                review.getStatus(),
                                review.isDeleted(),
                                pending.getOrDefault(review.getId(), 0L),
                                review.getAuthorId() == null ? null : authors.get(review.getAuthorId()),
                                review.getCreatedAt()))
                        .toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
    }

    private static String normalize(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    private static PageRequest pageRequest(int page) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE);
    }
}
