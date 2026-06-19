package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// JpaAuditingConfig: RequirementSet은 BaseEntity(생성/수정시각 감사) 상속
// QueryDslConfig: 컨텍스트가 다른 커스텀 리포지토리 구현(JPAQueryFactory 의존)을 초기화하므로 필요
@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class RequirementSetRepositoryTest {

    @Autowired
    private RequirementSetRepository requirementSetRepository;

    @Autowired
    private GraduationRuleRepository graduationRuleRepository;

    @Autowired
    private EntityManager entityManager;

    private Long ruleId(String name) {
        return graduationRuleRepository
                .save(GraduationRule.create(1L, name, "{}", null))
                .getId();
    }

    @Test
    void 요건세트에_연결한_규칙이_조인테이블로_저장되고_조회된다() {
        GraduationRule a = graduationRuleRepository.save(GraduationRule.create(1L, "규칙A", "{}", null));
        GraduationRule b = graduationRuleRepository.save(GraduationRule.create(2L, "규칙B", "{}", null));

        RequirementSet set = RequirementSet.create(1L, 2023, 2025, 1, "설명", null, true);
        set.replaceRules(List.of(a, b));
        Long setId = requirementSetRepository.save(set).getId();

        entityManager.flush();
        entityManager.clear();

        RequirementSet found = requirementSetRepository.findById(setId).orElseThrow();
        assertThat(found.getRules()).extracting(GraduationRule::getRuleName).containsExactlyInAnyOrder("규칙A", "규칙B");
    }

    @Test
    void 규칙_목록을_교체하면_조인테이블이_갱신된다() {
        GraduationRule a = graduationRuleRepository.save(GraduationRule.create(1L, "규칙A", "{}", null));
        GraduationRule b = graduationRuleRepository.save(GraduationRule.create(2L, "규칙B", "{}", null));

        RequirementSet set = RequirementSet.create(1L, 2023, 2025, 1, null, null, true);
        set.replaceRules(List.of(a, b));
        Long setId = requirementSetRepository.save(set).getId();
        entityManager.flush();
        entityManager.clear();

        RequirementSet reloaded = requirementSetRepository.findById(setId).orElseThrow();
        reloaded.replaceRules(List.of(a)); // B 해제
        requirementSetRepository.save(reloaded);
        entityManager.flush();
        entityManager.clear();

        RequirementSet result = requirementSetRepository.findById(setId).orElseThrow();
        assertThat(result.getRules()).extracting(GraduationRule::getRuleName).containsExactly("규칙A");
    }

    @Test
    void 학과_연도_버전으로_단건_조회한다() {
        ruleId("기타");
        RequirementSet set = RequirementSet.create(5L, 2023, 2025, 2, null, null, true);
        requirementSetRepository.save(set);

        var found = requirementSetRepository.findByDepartmentIdAndYearStartAndYearEndAndVersion(5L, 2023, 2025, 2);

        assertThat(found).isPresent();
    }

    @Test
    void 학과와_단일연도로_동적_검색한다() {
        requirementSetRepository.save(RequirementSet.create(1L, 2023, 2025, 1, null, null, true));
        requirementSetRepository.save(RequirementSet.create(1L, 2026, 2027, 1, null, null, true));
        requirementSetRepository.save(RequirementSet.create(2L, 2023, 2025, 1, null, null, true));

        // 학과1 + 2024 → 2023~2025만 포함(2026~2027 제외)
        assertThat(requirementSetRepository.search(1L, 2024)).hasSize(1);
        // 연도만 2024 → 학과1·학과2의 2023~2025 = 2건
        assertThat(requirementSetRepository.search(null, 2024)).hasSize(2);
        // 필터 없음 → 전체 3건
        assertThat(requirementSetRepository.search(null, null)).hasSize(3);
    }
}
