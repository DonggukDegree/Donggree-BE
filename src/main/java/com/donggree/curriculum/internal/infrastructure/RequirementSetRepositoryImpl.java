package com.donggree.curriculum.internal.infrastructure;

import com.donggree.curriculum.internal.application.projection.RequirementSetSummaryProjection;
import com.donggree.curriculum.internal.domain.department.QDepartment;
import com.donggree.curriculum.internal.domain.requirementset.QRequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link RequirementSetRepositoryCustom}의 QueryDSL 구현(infrastructure 어댑터).
 * 엔티티를 로딩하지 않고 department를 left join해 departmentName을 채운 요약 프로젝션으로 바로 조회한다.
 * 각 필터 조건은 값이 null이면 null Predicate를 반환하고, where()가 null을 무시하여 동적 조회가 된다.
 */
@Repository
@RequiredArgsConstructor
public class RequirementSetRepositoryImpl implements RequirementSetRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RequirementSetSummaryProjection> search(List<Long> departmentIds, Integer year) {
        QRequirementSet rs = QRequirementSet.requirementSet;
        QDepartment d = QDepartment.department;
        return queryFactory
                .select(Projections.constructor(
                        RequirementSetSummaryProjection.class,
                        rs.id,
                        rs.departmentId,
                        d.departmentName,
                        rs.yearStart,
                        rs.yearEnd,
                        rs.version,
                        rs.description,
                        rs.sheetImageUrl,
                        rs.active))
                .from(rs)
                .leftJoin(d)
                .on(d.id.eq(rs.departmentId))
                .where(departmentIn(rs, departmentIds), yearWithin(rs, year))
                .orderBy(rs.departmentId.asc(), rs.yearStart.asc(), rs.version.asc())
                .fetch();
    }

    private BooleanExpression departmentIn(QRequirementSet rs, List<Long> departmentIds) {
        return (departmentIds == null || departmentIds.isEmpty()) ? null : rs.departmentId.in(departmentIds);
    }

    private BooleanExpression yearWithin(QRequirementSet rs, Integer year) {
        return year == null ? null : rs.yearStart.loe(year).and(rs.yearEnd.goe(year));
    }
}
