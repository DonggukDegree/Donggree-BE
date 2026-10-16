package com.donggree.curriculum.internal.application.graduationrule;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.readmodel.GraduationRuleReadModel;
import java.util.List;

/**
 * 유형명과 이수구분을 포함한 졸업 규칙 조회 계약.
 */
public interface GraduationRuleQueries {

    /**
     * 동적 다중 필터로 졸업 규칙을 조회한다. 각 파라미터가 null이거나 빈 목록이면 해당 조건은 무시한다(미지정=전체).
     * requirementSetId가 주어지면 그 세트에 연결된 규칙만 조회한다.
     * 정렬: 이수구분 오름차순(null은 마지막), 규칙 유형 ID 오름차순.
     */
    List<GraduationRuleReadModel> search(List<Long> ruleTypeIds, List<CourseType> courseTypes, Long requirementSetId);
}
