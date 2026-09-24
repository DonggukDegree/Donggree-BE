package com.donggree.curriculum.internal.domain.graduationrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.GraduationRuleProjection;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import com.donggree.curriculum.internal.infrastructure.GraduationRuleRepositoryImpl;
import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QueryDslConfig.class, JpaAuditingConfig.class, GraduationRuleRepositoryImpl.class})
class GraduationRuleRepositoryTest {

    @Autowired
    private GraduationRuleRepository graduationRuleRepository;

    @Autowired
    private GraduationRuleRepositoryCustom customRepository;

    @Autowired
    private RuleTypeRepository ruleTypeRepository;

    @Autowired
    private RequirementSetRepository requirementSetRepository;

    private Long generalTypeId; // course_type = null
    private Long majorTypeId; // FIRST_MAJOR
    private Long liberalTypeId; // LIBERAL_ARTS

    @BeforeEach
    void setUp() {
        generalTypeId = ruleTypeRepository
                .save(RuleType.create("TOTAL_CREDITS", null, null))
                .getId();
        majorTypeId = ruleTypeRepository
                .save(RuleType.create("MIN_AREA_CREDITS", CourseType.FIRST_MAJOR, null))
                .getId();
        liberalTypeId = ruleTypeRepository
                .save(RuleType.create("REQUIRED_COURSE", CourseType.LIBERAL_ARTS, null))
                .getId();

        graduationRuleRepository.save(GraduationRule.create(generalTypeId, "총학점", "{}", null));
        graduationRuleRepository.save(GraduationRule.create(majorTypeId, "전공영역", "{}", null));
        graduationRuleRepository.save(GraduationRule.create(liberalTypeId, "교양필수", "{}", null));
    }

    @Test
    void 필터가_없으면_course_type_그다음_rule_type_id_순으로_전체_조회한다() {
        List<GraduationRuleProjection> result = customRepository.search(null, null, null);

        // course_type asc(NULLS LAST): FIRST_MAJOR(전공영역) < LIBERAL_ARTS(교양필수) < null(총학점)
        assertThat(result).extracting(GraduationRuleProjection::ruleName).containsExactly("전공영역", "교양필수", "총학점");
    }

    @Test
    void 조회_결과에_rule_type을_조인해_typeName과_courseType을_채운다() {
        GraduationRuleProjection major =
                customRepository.search(List.of(majorTypeId), null, null).get(0);

        assertThat(major.typeName()).isEqualTo("MIN_AREA_CREDITS");
        assertThat(major.courseType()).isEqualTo(CourseType.FIRST_MAJOR);
        assertThat(major.ruleName()).isEqualTo("전공영역");
    }

    @Test
    void ruleTypeId로_필터링한다() {
        List<GraduationRuleProjection> result = customRepository.search(List.of(majorTypeId), null, null);

        assertThat(result).extracting(GraduationRuleProjection::ruleName).containsExactly("전공영역");
    }

    @Test
    void courseType으로_필터링한다() {
        List<GraduationRuleProjection> result = customRepository.search(null, List.of(CourseType.LIBERAL_ARTS), null);

        assertThat(result).extracting(GraduationRuleProjection::ruleName).containsExactly("교양필수");
    }

    @Test
    void courseType을_여러_개_지정하면_OR로_정렬되어_조회한다() {
        List<GraduationRuleProjection> result =
                customRepository.search(null, List.of(CourseType.FIRST_MAJOR, CourseType.LIBERAL_ARTS), null);

        assertThat(result).extracting(GraduationRuleProjection::ruleName).containsExactly("전공영역", "교양필수");
    }

    @Test
    void requirementSetId로_세트에_연결된_규칙만_조회한다() {
        // setUp의 3개 규칙 중 2개만 한 세트에 연결
        List<GraduationRule> all = graduationRuleRepository.findAll();
        GraduationRule linked1 = all.get(0);
        GraduationRule linked2 = all.get(1);
        RequirementSet set = RequirementSet.create(1L, 2023, 2025, RequirementTrack.ALL, 1, null, null, true);
        set.replaceRules(List.of(linked1, linked2));
        Long setId = requirementSetRepository.save(set).getId();

        List<GraduationRuleProjection> result = customRepository.search(null, null, setId);

        assertThat(result)
                .extracting(GraduationRuleProjection::id)
                .containsExactlyInAnyOrder(linked1.getId(), linked2.getId());
    }

    @Test
    void 규칙종류와_이름으로_옵션이_다른_규칙도_함께_조회한다() {
        graduationRuleRepository.saveAndFlush(
                GraduationRule.create(generalTypeId, "총학점", "{\"minCredits\":130}", null));
        var found = graduationRuleRepository.findAllByRuleTypeIdAndRuleName(generalTypeId, "총학점");

        assertThat(found).hasSize(2);
        assertThat(found).extracting(GraduationRule::getId).doesNotHaveDuplicates();
    }

    @Test
    void 종류_이름_옵션이_같으면_설명이_달라도_DB에서_거절한다() {
        assertThatThrownBy(() -> graduationRuleRepository.saveAndFlush(
                        GraduationRule.create(generalTypeId, "총학점", "{}", "다른 설명")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void 역할_순서만_다른_규칙은_DB에서도_중복이다() {
        graduationRuleRepository.saveAndFlush(GraduationRule.create(
                majorTypeId,
                "역할",
                "{\"minCredits\":36,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\"]}",
                null));
        assertThatThrownBy(() -> graduationRuleRepository.saveAndFlush(GraduationRule.create(
                        majorTypeId,
                        "역할",
                        "{\"applicableMajorRoles\":[\"DUAL_PRIMARY\",\"SINGLE_PRIMARY\"],\"minCredits\":36.0}",
                        "다른 설명")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
