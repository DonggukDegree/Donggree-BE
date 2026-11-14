package com.donggree.place.internal.application;

import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.application.readmodel.RatingSummary;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceTag;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 장소 읽기 모델을 반환하는 응용 계층 조회 계약. */
public interface PlaceQueries {

    /**
     * 지도에 표시할 숨기지 않은 장소 전체를 조회한다.
     * 서비스 영역이 캠퍼스 반경 1.5km라 화면 영역별 조회 없이 한 번에 내려준다.
     */
    List<PlaceSummaryReadModel> findVisibleSummaries();

    /** 리뷰가 있는 장소별 별점 요약. 숨기거나 지운 리뷰는 세지 않는다. */
    Map<Long, RatingSummary> findRatingSummaries();

    /** 숨기지 않은 장소의 숨은 맛집 태그(장소별, enum 순서). 태그가 없는 장소는 넣지 않는다. */
    Map<Long, List<PlaceTag>> findVisiblePlaceTags();

    RatingSummary findRatingSummary(Long placeId);

    /**
     * 장소 리뷰 목록에 놓을 리뷰를 최신순(ID 내림차순)으로 조회한다. 숨긴 리뷰도 자리를 지키도록 포함하고,
     * 작성자가 지운 리뷰만 뺀다. 숨긴 리뷰의 본문·별점은 응용 서비스가 가린다.
     *
     * @param cursor 이 ID보다 오래된 리뷰부터. null이면 처음부터
     * @param limit  최대 개수
     */
    List<PlaceReview> findListedReviews(Long placeId, Long cursor, int limit);

    /**
     * 관리자 장소 목록(최신 등록순).
     *
     * @param status  null이면 전체
     * @param keyword 장소 이름 부분 일치. null이면 전체
     */
    Page<Place> findPlacesForAdmin(PlaceStatus status, String keyword, Pageable pageable);

    /**
     * 관리자 리뷰 목록(최신 작성순). 작성자가 지운 리뷰도 포함한다.
     *
     * @param state   null이면 전체
     * @param keyword 리뷰 본문 또는 장소 이름 부분 일치. null이면 전체
     */
    Page<PlaceReview> findReviewsForAdmin(AdminReviewState state, String keyword, Pageable pageable);

    /** 장소별 처리 대기 신고 수. 대기 신고가 없는 장소는 넣지 않는다. */
    Map<Long, Long> countPendingPlaceReports(Collection<Long> placeIds);

    /** 리뷰별 처리 대기 신고 수. 대기 신고가 없는 리뷰는 넣지 않는다. */
    Map<Long, Long> countPendingReviewReports(Collection<Long> reviewIds);

    /** 관리자 리뷰 목록의 상태 필터. 숨김과 작성자 삭제는 따로 저장되므로 화면에서 볼 상태로 묶는다. */
    enum AdminReviewState {
        /** 보이는 리뷰(숨김 아님, 삭제 안 됨) */
        VISIBLE,
        /** 신고 누적·관리자로 숨긴 리뷰(삭제 안 됨) */
        HIDDEN,
        /** 작성자가 지운 리뷰 */
        DELETED
    }
}
