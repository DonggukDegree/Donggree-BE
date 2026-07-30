package com.donggree.curriculum.internal.domain.graduationrule;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.GraduationRuleProjection;
import java.util.List;

/**
 * 졸업 규칙 동적 조회를 위한 커스텀 리포지토리. QueryDSL 구현은 infrastructure에 둔다(DIP).
 * 엔티티를 로딩하지 않고 rule_type을 조인해 읽기 전용 프로젝션으로 바로 조회한다.
 */
public interface GraduationRuleRepositoryCustom {

    /**
     * 동적 다중 필터로 졸업 규칙을 조회한다. 각 목록이 null이거나 비어 있으면 해당 조건은 무시한다(미지정=전체).
     * courseType은 rule_type 소유 컬럼이므로 rule_type과 조인하여 필터·정렬하고 typeName·courseType을 함께 채운다.
     * 정렬: rule_type.course_type 오름차순(NULLS LAST), rule_type_id 오름차순.
     */
    List<GraduationRuleProjection> search(List<Long> ruleTypeIds, List<CourseType> courseTypes);
}
