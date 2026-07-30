package com.donggree.curriculum.internal.infrastructure;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.CourseClassificationProjection;
import com.donggree.curriculum.internal.domain.areatype.QAreaType;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassificationRepositoryCustom;
import com.donggree.curriculum.internal.domain.courseclassification.QCourseClassification;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link CourseClassificationRepositoryCustom}의 QueryDSL 구현.
 * 엔티티를 로딩하지 않고 area_type을 left join해 프로젝션으로 바로 조회한다(areaName은 미등록 시 null).
 * 각 필터 조건은 목록이 null이거나 비어 있으면 null Predicate를 반환하고, QueryDSL where()가 null을 무시하여 동적 조회가 된다.
 */
@Repository
@RequiredArgsConstructor
public class CourseClassificationRepositoryImpl implements CourseClassificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CourseClassificationProjection> search(
            List<Long> areaTypeIds, List<CourseType> courseTypes, List<Integer> years) {
        QCourseClassification cc = QCourseClassification.courseClassification;
        QAreaType at = QAreaType.areaType;
        return queryFactory
                .select(Projections.constructor(
                        CourseClassificationProjection.class,
                        cc.id,
                        cc.courseCode,
                        cc.tag,
                        cc.studentYearStart,
                        cc.studentYearEnd,
                        cc.courseType,
                        cc.areaTypeId,
                        at.areaName,
                        cc.subCategory,
                        cc.subjectDomain))
                .from(cc)
                .leftJoin(at)
                .on(at.id.eq(cc.areaTypeId))
                .where(areaTypeIn(cc, areaTypeIds), courseTypeIn(cc, courseTypes), yearsContain(cc, years))
                .orderBy(cc.courseCode.asc(), cc.studentYearStart.asc())
                .fetch();
    }

    private BooleanExpression areaTypeIn(QCourseClassification cc, List<Long> areaTypeIds) {
        return isEmpty(areaTypeIds) ? null : cc.areaTypeId.in(areaTypeIds);
    }

    private BooleanExpression courseTypeIn(QCourseClassification cc, List<CourseType> courseTypes) {
        return isEmpty(courseTypes) ? null : cc.courseType.in(courseTypes);
    }

    /** 지정한 연도들 중 하나라도 적용 범위(start≤year≤end)에 포함되면 매칭한다(OR). */
    private BooleanExpression yearsContain(QCourseClassification cc, List<Integer> years) {
        if (isEmpty(years)) {
            return null;
        }
        BooleanExpression combined = null;
        for (Integer year : years) {
            BooleanExpression contains = cc.studentYearStart.loe(year).and(cc.studentYearEnd.goe(year));
            combined = (combined == null) ? contains : combined.or(contains);
        }
        return combined;
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
