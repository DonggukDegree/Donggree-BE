package com.donggree.curriculum.internal.domain;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@link RequirementSetRepositoryCustom}의 QueryDSL 구현.
 * 각 필터 조건은 값이 null이면 null Predicate를 반환하고, where()가 null을 무시하여 동적 조회가 된다.
 */
@RequiredArgsConstructor
public class RequirementSetRepositoryImpl implements RequirementSetRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RequirementSet> search(Long departmentId, Integer year) {
        QRequirementSet rs = QRequirementSet.requirementSet;
        return queryFactory
                .selectFrom(rs)
                .where(departmentEq(rs, departmentId), yearWithin(rs, year))
                .orderBy(rs.departmentId.asc(), rs.yearStart.asc(), rs.version.asc())
                .fetch();
    }

    private BooleanExpression departmentEq(QRequirementSet rs, Long departmentId) {
        return departmentId == null ? null : rs.departmentId.eq(departmentId);
    }

    private BooleanExpression yearWithin(QRequirementSet rs, Integer year) {
        return year == null ? null : rs.yearStart.loe(year).and(rs.yearEnd.goe(year));
    }
}
