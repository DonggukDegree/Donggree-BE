package com.donggree.place.internal.application;

import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.application.readmodel.RatingSummary;
import com.donggree.place.internal.domain.PlaceReview;
import java.util.List;
import java.util.Map;

/** 장소 읽기 모델을 반환하는 응용 계층 조회 계약. */
public interface PlaceQueries {

    /**
     * 지도에 표시할 숨기지 않은 장소 전체를 조회한다.
     * 서비스 영역이 캠퍼스 반경 1.5km라 화면 영역별 조회 없이 한 번에 내려준다.
     */
    List<PlaceSummaryReadModel> findVisibleSummaries();

    /** 리뷰가 있는 장소별 별점 요약. 숨기거나 지운 리뷰는 세지 않는다. */
    Map<Long, RatingSummary> findRatingSummaries();

    RatingSummary findRatingSummary(Long placeId);

    /**
     * 장소의 보이는 리뷰를 최신순(ID 내림차순)으로 조회한다.
     *
     * @param cursor 이 ID보다 오래된 리뷰부터. null이면 처음부터
     * @param limit  최대 개수
     */
    List<PlaceReview> findVisibleReviews(Long placeId, Long cursor, int limit);
}
