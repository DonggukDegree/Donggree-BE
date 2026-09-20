package com.donggree.curriculum.internal.domain.requirementset;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.internal.application.projection.RequirementSetSummaryProjection;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.infrastructure.RequirementSetRepositoryImpl;
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
@Import({JpaAuditingConfig.class, QueryDslConfig.class, RequirementSetRepositoryImpl.class})
class RequirementSetRepositoryTest {

    @Autowired
    private RequirementSetRepository requirementSetRepository;

    @Autowired
    private GraduationRuleRepository graduationRuleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RequirementSetRepositoryCustom customRepository;

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

        RequirementSet set = RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, 1, "설명", null, true);
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

        RequirementSet set = RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, 1, null, null, true);
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
    void 같은_학과_적용년도의_최신_버전을_조회한다() {
        requirementSetRepository.save(
                RequirementSet.create(5L, 2023, 2025, RequirementTrack.ALL, 1, null, null, false));
        requirementSetRepository.save(
                RequirementSet.create(5L, 2023, 2025, RequirementTrack.ALL, 2, null, null, false));
        requirementSetRepository.save(
                RequirementSet.create(5L, 2026, 2027, RequirementTrack.ALL, 1, null, null, false));

        var latest = requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndAndTrackOrderByVersionDesc(
                5L, 2023, 2025, RequirementTrack.ALL);
        assertThat(latest).isPresent();
        assertThat(latest.get().getVersion()).isEqualTo(2);

        var none = requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndAndTrackOrderByVersionDesc(
                5L, 2030, 2031, RequirementTrack.ALL);
        assertThat(none).isEmpty();
    }

    @Test
    void 버전_lineage는_과정별로_따로_매겨진다() {
        requirementSetRepository.save(
                RequirementSet.create(7L, 2023, 2025, RequirementTrack.GENERAL, 1, null, null, false));
        requirementSetRepository.save(
                RequirementSet.create(7L, 2023, 2025, RequirementTrack.GENERAL, 2, null, null, false));
        requirementSetRepository.save(
                RequirementSet.create(7L, 2023, 2025, RequirementTrack.ADVANCED, 1, null, null, false));

        var general = requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndAndTrackOrderByVersionDesc(
                7L, 2023, 2025, RequirementTrack.GENERAL);
        var advanced = requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndAndTrackOrderByVersionDesc(
                7L, 2023, 2025, RequirementTrack.ADVANCED);

        assertThat(general).isPresent();
        assertThat(general.get().getVersion()).isEqualTo(2);
        assertThat(advanced).isPresent();
        assertThat(advanced.get().getVersion()).isEqualTo(1);
    }

    @Test
    void 겹침_검사는_적용대상이_겹치는_과정만_본다() {
        // 일반과정 활성 세트 하나만 등록된 상태
        requirementSetRepository.save(
                RequirementSet.create(8L, 2023, 2025, RequirementTrack.GENERAL, 1, null, null, true));

        // 심화과정 세트를 추가하려는 경우 → 검사 대상이 {ALL, ADVANCED}라 걸리지 않는다
        assertThat(
                        requirementSetRepository
                                .existsByActiveTrueAndDepartmentIdAndTrackInAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                                        8L, RequirementTrack.ADVANCED.conflictingTracks(), 2025, 2023))
                .isFalse();

        // 또 다른 일반과정 세트를 추가하려는 경우 → 같은 학생을 보므로 걸린다
        assertThat(
                        requirementSetRepository
                                .existsByActiveTrueAndDepartmentIdAndTrackInAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                                        8L, RequirementTrack.GENERAL.conflictingTracks(), 2025, 2023))
                .isTrue();

        // ALL 세트를 추가하려는 경우 → 모든 학생을 받아 일반과정 학생이 겹치므로 걸린다
        assertThat(
                        requirementSetRepository
                                .existsByActiveTrueAndDepartmentIdAndTrackInAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                                        8L, RequirementTrack.ALL.conflictingTracks(), 2025, 2023))
                .isTrue();
    }

    @Test
    void 학과의_활성_세트만_조회한다() {
        requirementSetRepository.save(RequirementSet.create(9L, 2023, 2024, RequirementTrack.ALL, 1, null, null, true));
        requirementSetRepository.save(
                RequirementSet.create(9L, 2025, 2026, RequirementTrack.ALL, 1, null, null, false));

        assertThat(requirementSetRepository.findByDepartmentIdAndActiveTrue(9L)).hasSize(1);
    }

    @Test
    void 학과묶음과_단일연도로_동적_검색한다() {
        requirementSetRepository.save(RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, 1, null, null, true));
        requirementSetRepository.save(RequirementSet.create(1L, 2026, 2027, RequirementTrack.ALL, 1, null, null, true));
        requirementSetRepository.save(RequirementSet.create(2L, 2023, 2025, RequirementTrack.ALL, 1, null, null, true));

        // 학과1 + 2024 → 2023~2025만 포함(2026~2027 제외)
        assertThat(customRepository.search(List.of(1L), 2024)).hasSize(1);
        // 학과1·학과2 묶음 + 2024 → 두 학과의 2023~2025 = 2건
        assertThat(customRepository.search(List.of(1L, 2L), 2024)).hasSize(2);
        // 연도만 2024(학과 필터 없음) → 학과1·학과2의 2023~2025 = 2건
        assertThat(customRepository.search(null, 2024)).hasSize(2);
        // 필터 없음 → 전체 3건
        assertThat(customRepository.search(null, null)).hasSize(3);
    }

    @Test
    void 검색_결과에_department를_조인해_학과명을_채운다() {
        Department dept = departmentRepository.save(Department.create(1L, "컴퓨터·AI학부"));
        requirementSetRepository.save(
                RequirementSet.create(dept.getId(), 2023, 2025, RequirementTrack.ALL, 1, null, null, true));

        List<RequirementSetSummaryProjection> result = customRepository.search(List.of(dept.getId()), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).departmentName()).isEqualTo("컴퓨터·AI학부");
        assertThat(result.get(0).departmentId()).isEqualTo(dept.getId());
    }
}
