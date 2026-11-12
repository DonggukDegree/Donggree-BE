package com.donggree.place.internal.infrastructure;

import com.donggree.place.internal.application.PlaceQueries;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.application.readmodel.RatingSummary;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.QPlace;
import com.donggree.place.internal.domain.QPlaceReview;
import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** {@link PlaceQueries}의 QueryDSL 구현. 메뉴를 로딩하지 않고 필요한 열만 조회하며, 별점은 리뷰를 집계한다. */
@Repository
@RequiredArgsConstructor
public class QuerydslPlaceQueries implements PlaceQueries {

    private static final QPlace PLACE = QPlace.place;
    private static final QPlaceReview REVIEW = QPlaceReview.placeReview;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PlaceSummaryReadModel> findVisibleSummaries() {
        return queryFactory
                .select(PLACE.id, PLACE.name, PLACE.category, PLACE.subCategory, PLACE.latitude, PLACE.longitude)
                .from(PLACE)
                .where(PLACE.status.eq(PlaceStatus.VISIBLE))
                .orderBy(PLACE.id.asc())
                .fetch()
                .stream()
                .map(row -> new PlaceSummaryReadModel(
                        row.get(PLACE.id),
                        row.get(PLACE.name),
                        row.get(PLACE.category),
                        row.get(PLACE.subCategory),
                        row.get(PLACE.latitude),
                        row.get(PLACE.longitude),
                        null,
                        0))
                .toList();
    }

    @Override
    public Map<Long, RatingSummary> findRatingSummaries() {
        return queryFactory
                .select(REVIEW.placeId, REVIEW.rating.avg(), REVIEW.count())
                .from(REVIEW)
                .where(visibleReview())
                .groupBy(REVIEW.placeId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(row -> row.get(REVIEW.placeId), QuerydslPlaceQueries::rating));
    }

    @Override
    public RatingSummary findRatingSummary(Long placeId) {
        Tuple row = queryFactory
                .select(REVIEW.rating.avg(), REVIEW.count())
                .from(REVIEW)
                .where(visibleReview(), REVIEW.placeId.eq(placeId))
                .fetchOne();
        return row == null ? RatingSummary.EMPTY : rating(row);
    }

    @Override
    public List<PlaceReview> findVisibleReviews(Long placeId, Long cursor, int limit) {
        return queryFactory
                .selectFrom(REVIEW)
                .where(visibleReview(), REVIEW.placeId.eq(placeId), cursor == null ? null : REVIEW.id.lt(cursor))
                .orderBy(REVIEW.id.desc())
                .limit(limit)
                .fetch();
    }

    private static BooleanExpression visibleReview() {
        return REVIEW.status.eq(PlaceReviewStatus.VISIBLE).and(REVIEW.deletedAt.isNull());
    }

    private static RatingSummary rating(Tuple row) {
        Long count = row.get(REVIEW.count());
        return RatingSummary.of(row.get(REVIEW.rating.avg()), count == null ? 0 : count);
    }
}
