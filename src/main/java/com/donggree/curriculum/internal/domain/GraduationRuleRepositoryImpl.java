package com.donggree.curriculum.internal.domain;

import com.donggree.curriculum.CourseType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@link GraduationRuleRepositoryCustom}의 QueryDSL 구현.
 * course_type 필터·정렬을 위해 rule_type과 엔티티 조인한다(같은 모듈 소유).
 * 각 필터 조건은 목록이 null이거나 비어 있으면 null Predicate를 반환하고, where()가 null을 무시하여 동적 조회가 된다.
 */
@RequiredArgsConstructor
public class GraduationRuleRepositoryImpl implements GraduationRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<GraduationRule> search(List<Long> ruleTypeIds, List<CourseType> courseTypes) {
        QGraduationRule gr = QGraduationRule.graduationRule;
        QRuleType rt = QRuleType.ruleType;
        return queryFactory
                .selectFrom(gr)
                .join(rt)
                .on(rt.id.eq(gr.ruleTypeId))
                .where(ruleTypeIn(gr, ruleTypeIds), courseTypeIn(rt, courseTypes))
                .orderBy(rt.courseType.asc().nullsLast(), gr.ruleTypeId.asc())
                .fetch();
    }

    private BooleanExpression ruleTypeIn(QGraduationRule gr, List<Long> ruleTypeIds) {
        return isEmpty(ruleTypeIds) ? null : gr.ruleTypeId.in(ruleTypeIds);
    }

    private BooleanExpression courseTypeIn(QRuleType rt, List<CourseType> courseTypes) {
        return isEmpty(courseTypes) ? null : rt.courseType.in(courseTypes);
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
