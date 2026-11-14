package com.donggree.place.internal.infrastructure;

import com.donggree.place.internal.application.PlaceQueries;
import com.donggree.place.internal.application.PlaceQueries.AdminReviewState;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.application.readmodel.RatingSummary;
import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceReview;
import com.donggree.place.internal.domain.QPlace;
import com.donggree.place.internal.domain.QPlaceReport;
import com.donggree.place.internal.domain.QPlaceReview;
import com.donggree.place.internal.domain.QReviewReport;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceTag;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** {@link PlaceQueries}의 QueryDSL 구현. 메뉴를 로딩하지 않고 필요한 열만 조회하며, 별점은 리뷰를 집계한다. */
@Repository
@RequiredArgsConstructor
public class QuerydslPlaceQueries implements PlaceQueries {

    private static final QPlace PLACE = QPlace.place;
    private static final QPlaceReview REVIEW = QPlaceReview.placeReview;
    private static final QPlaceReport PLACE_REPORT = QPlaceReport.placeReport;
    private static final QReviewReport REVIEW_REPORT = QReviewReport.reviewReport;

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
                        0,
                        List.of()))
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
    public Map<Long, List<PlaceTag>> findVisiblePlaceTags() {
        EnumPath<PlaceTag> tag = Expressions.enumPath(PlaceTag.class, "tag");
        return queryFactory
                .select(PLACE.id, tag)
                .from(PLACE)
                .join(PLACE.tags, tag)
                .where(PLACE.status.eq(PlaceStatus.VISIBLE))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(PLACE.id),
                        Collectors.collectingAndThen(
                                Collectors.mapping(row -> row.get(tag), Collectors.toList()),
                                list -> list.stream().sorted().toList())));
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
    public List<PlaceReview> findListedReviews(Long placeId, Long cursor, int limit) {
        return queryFactory
                .selectFrom(REVIEW)
                .where(
                        REVIEW.deletedAt.isNull(),
                        REVIEW.placeId.eq(placeId),
                        cursor == null ? null : REVIEW.id.lt(cursor))
                .orderBy(REVIEW.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<Place> findPlacesForAdmin(PlaceStatus status, String keyword, Pageable pageable) {
        BooleanExpression[] conditions = {
            status == null ? null : PLACE.status.eq(status), keyword == null ? null : PLACE.name.contains(keyword)
        };
        List<Place> content = queryFactory
                .selectFrom(PLACE)
                .where(conditions)
                .orderBy(PLACE.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total =
                queryFactory.select(PLACE.count()).from(PLACE).where(conditions).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<PlaceReview> findReviewsForAdmin(AdminReviewState state, String keyword, Pageable pageable) {
        BooleanExpression[] conditions = {stateCondition(state), keyword == null ? null : reviewKeyword(keyword)};
        List<PlaceReview> content = queryFactory
                .selectFrom(REVIEW)
                .where(conditions)
                .orderBy(REVIEW.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory
                .select(REVIEW.count())
                .from(REVIEW)
                .where(conditions)
                .fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Map<Long, Long> countPendingPlaceReports(Collection<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
                .select(PLACE_REPORT.placeId, PLACE_REPORT.count())
                .from(PLACE_REPORT)
                .where(PLACE_REPORT.placeId.in(placeIds), PLACE_REPORT.status.eq(PlaceReportStatus.PENDING))
                .groupBy(PLACE_REPORT.placeId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(row -> row.get(PLACE_REPORT.placeId), row -> row.get(PLACE_REPORT.count())));
    }

    @Override
    public Map<Long, Long> countPendingReviewReports(Collection<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
                .select(REVIEW_REPORT.reviewId, REVIEW_REPORT.count())
                .from(REVIEW_REPORT)
                .where(REVIEW_REPORT.reviewId.in(reviewIds), REVIEW_REPORT.status.eq(PlaceReportStatus.PENDING))
                .groupBy(REVIEW_REPORT.reviewId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        row -> row.get(REVIEW_REPORT.reviewId), row -> row.get(REVIEW_REPORT.count())));
    }

    private static BooleanExpression stateCondition(AdminReviewState state) {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case VISIBLE -> visibleReview();
            case HIDDEN -> REVIEW.status.eq(PlaceReviewStatus.HIDDEN).and(REVIEW.deletedAt.isNull());
            case DELETED -> REVIEW.deletedAt.isNotNull();
        };
    }

    // 본문 또는 장소 이름. 장소는 같은 모듈이라 하위 조회로 묶는다.
    private static BooleanExpression reviewKeyword(String keyword) {
        return REVIEW.content
                .contains(keyword)
                .or(REVIEW.placeId.in(
                        JPAExpressions.select(PLACE.id).from(PLACE).where(PLACE.name.contains(keyword))));
    }

    private static BooleanExpression visibleReview() {
        return REVIEW.status.eq(PlaceReviewStatus.VISIBLE).and(REVIEW.deletedAt.isNull());
    }

    private static RatingSummary rating(Tuple row) {
        Long count = row.get(REVIEW.count());
        return RatingSummary.of(row.get(REVIEW.rating.avg()), count == null ? 0 : count);
    }
}
