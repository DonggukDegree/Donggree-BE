package com.donggree.place.internal.infrastructure;

import com.donggree.place.internal.application.PlaceQueries;
import com.donggree.place.internal.application.readmodel.PlaceSummaryReadModel;
import com.donggree.place.internal.domain.QPlace;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** {@link PlaceQueries}의 QueryDSL 구현. 메뉴를 로딩하지 않고 핀 표시에 필요한 열만 프로젝션한다. */
@Repository
@RequiredArgsConstructor
public class QuerydslPlaceQueries implements PlaceQueries {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PlaceSummaryReadModel> findVisibleSummaries() {
        QPlace place = QPlace.place;
        return queryFactory
                .select(Projections.constructor(
                        PlaceSummaryReadModel.class,
                        place.id,
                        place.name,
                        place.category,
                        place.subCategory,
                        place.latitude,
                        place.longitude))
                .from(place)
                .where(place.status.eq(PlaceStatus.VISIBLE))
                .orderBy(place.id.asc())
                .fetch();
    }
}
