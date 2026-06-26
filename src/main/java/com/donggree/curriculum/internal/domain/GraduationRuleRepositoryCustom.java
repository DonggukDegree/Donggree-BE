package com.donggree.curriculum.internal.domain;

import com.donggree.curriculum.CourseType;
import java.util.List;

/**
 * 졸업 규칙 동적 조회를 위한 커스텀 리포지토리. QueryDSL로 구현한다.
 */
public interface GraduationRuleRepositoryCustom {

    /**
     * 동적 다중 필터로 졸업 규칙을 조회한다. 각 목록이 null이거나 비어 있으면 해당 조건은 무시한다(미지정=전체).
     * courseType은 rule_type 소유 컬럼이므로 rule_type과 조인하여 필터·정렬한다.
     * 정렬: rule_type.course_type 오름차순(NULLS LAST), rule_type_id 오름차순.
     */
    List<GraduationRule> search(List<Long> ruleTypeIds, List<CourseType> courseTypes);
}
