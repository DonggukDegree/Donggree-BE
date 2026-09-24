package com.donggree.curriculum.internal.domain.graduationrule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationRuleRepository extends JpaRepository<GraduationRule, Long> {

    /** 이름이 같은 규칙도 옵션이 다르면 공존하므로, 중복 비교 후보를 목록으로 조회한다. */
    List<GraduationRule> findAllByRuleTypeIdAndRuleName(Long ruleTypeId, String ruleName);
}
