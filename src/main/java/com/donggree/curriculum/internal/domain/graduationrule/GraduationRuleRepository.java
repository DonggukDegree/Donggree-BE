package com.donggree.curriculum.internal.domain.graduationrule;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationRuleRepository extends JpaRepository<GraduationRule, Long> {

    /** 규칙 종류 + 규칙 이름으로 조회한다. 등록/수정 시 unique 제약 충돌 사전 검증에 사용한다. */
    Optional<GraduationRule> findByRuleTypeIdAndRuleName(Long ruleTypeId, String ruleName);
}
