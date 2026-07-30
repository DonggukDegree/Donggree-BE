package com.donggree.curriculum.internal.infrastructure;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.GraduationRuleProjection;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepositoryCustom;
import com.donggree.curriculum.internal.domain.graduationrule.QGraduationRule;
import com.donggree.curriculum.internal.domain.requirementset.QRequirementSet;
import com.donggree.curriculum.internal.domain.ruletype.QRuleType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * {@link GraduationRuleRepositoryCustom}의 QueryDSL 구현(infrastructure 어댑터).
 * 엔티티를 로딩하지 않고 rule_type과 조인해 typeName·courseType을 채운 프로젝션으로 바로 조회한다.
 * 각 필터 조건은 값이 null이거나 비어 있으면 null Predicate를 반환하고, where()가 null을 무시하여 동적 조회가 된다.
 */
@Repository
@RequiredArgsConstructor
public class GraduationRuleRepositoryImpl implements GraduationRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<GraduationRuleProjection> search(
            List<Long> ruleTypeIds, List<CourseType> courseTypes, Long requirementSetId) {
        QGraduationRule gr = QGraduationRule.graduationRule;
        QRuleType rt = QRuleType.ruleType;
        return queryFactory
                .select(Projections.constructor(
                        GraduationRuleProjection.class,
                        gr.id,
                        gr.ruleTypeId,
                        rt.typeName,
                        rt.courseType,
                        gr.ruleName,
                        gr.ruleConfig,
                        gr.description))
                .from(gr)
                .join(rt)
                .on(rt.id.eq(gr.ruleTypeId))
                .where(
                        ruleTypeIn(gr, ruleTypeIds),
                        courseTypeIn(rt, courseTypes),
                        inRequirementSet(gr, requirementSetId))
                .orderBy(rt.courseType.asc().nullsLast(), gr.ruleTypeId.asc())
                .fetch();
    }

    private BooleanExpression ruleTypeIn(QGraduationRule gr, List<Long> ruleTypeIds) {
        return isEmpty(ruleTypeIds) ? null : gr.ruleTypeId.in(ruleTypeIds);
    }

    private BooleanExpression courseTypeIn(QRuleType rt, List<CourseType> courseTypes) {
        return isEmpty(courseTypes) ? null : rt.courseType.in(courseTypes);
    }

    /**
     * requirement_set_rule 조인테이블로 특정 세트에 연결된 규칙만 남긴다.
     * GraduationRule에는 역방향 매핑이 없으므로, 소유 측(RequirementSet.rules)을 서브쿼리로 조인해 id를 걸러낸다.
     */
    private BooleanExpression inRequirementSet(QGraduationRule gr, Long requirementSetId) {
        if (requirementSetId == null) {
            return null;
        }
        QRequirementSet rs = QRequirementSet.requirementSet;
        QGraduationRule linked = new QGraduationRule("linkedRule");
        return gr.id.in(
                JPAExpressions.select(linked.id).from(rs).join(rs.rules, linked).where(rs.id.eq(requirementSetId)));
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
