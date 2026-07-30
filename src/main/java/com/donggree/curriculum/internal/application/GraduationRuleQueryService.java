package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.GraduationRuleProjection;
import com.donggree.curriculum.internal.application.projection.RuleTypeProjection;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepositoryCustom;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 졸업 규칙(graduation_rule)·규칙 종류(rule_type) 조회(Query) 응용 서비스.
 * 상태를 변경하지 않는 읽기 유스케이스만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class GraduationRuleQueryService {

    private final GraduationRuleRepositoryCustom graduationRuleRepository;
    private final RuleTypeRepository ruleTypeRepository;

    /** 규칙 종류 전체를 조회한다. 졸업 규칙 필터의 선택지로 사용된다. */
    @Transactional(readOnly = true)
    public List<RuleTypeProjection> getRuleTypes() {
        return ruleTypeRepository.findAll().stream()
                .map(rt ->
                        new RuleTypeProjection(rt.getId(), rt.getTypeName(), rt.getCourseType(), rt.getDescription()))
                .toList();
    }

    /**
     * 동적 다중 필터로 졸업 규칙을 조회한다. rule_type을 조인해 typeName·courseType을 함께 담은 프로젝션으로
     * 엔티티 로딩 없이 바로 조회한다. 각 필터 목록이 비어 있으면 전체를 반환한다.
     */
    @Transactional(readOnly = true)
    public List<GraduationRuleProjection> search(List<Long> ruleTypeIds, List<CourseType> courseTypes) {
        return graduationRuleRepository.search(ruleTypeIds, courseTypes);
    }
}
