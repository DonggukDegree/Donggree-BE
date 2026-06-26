package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.global.config.QueryDslConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QueryDslConfig.class)
class GraduationRuleRepositoryTest {

    @Autowired
    private GraduationRuleRepository graduationRuleRepository;

    @Autowired
    private RuleTypeRepository ruleTypeRepository;

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
        List<GraduationRule> result = graduationRuleRepository.search(null, null);

        // course_type asc(NULLS LAST): FIRST_MAJOR(전공영역) < LIBERAL_ARTS(교양필수) < null(총학점)
        assertThat(result).extracting(GraduationRule::getRuleName).containsExactly("전공영역", "교양필수", "총학점");
    }

    @Test
    void ruleTypeId로_필터링한다() {
        List<GraduationRule> result = graduationRuleRepository.search(List.of(majorTypeId), null);

        assertThat(result).extracting(GraduationRule::getRuleName).containsExactly("전공영역");
    }

    @Test
    void courseType으로_필터링한다() {
        List<GraduationRule> result = graduationRuleRepository.search(null, List.of(CourseType.LIBERAL_ARTS));

        assertThat(result).extracting(GraduationRule::getRuleName).containsExactly("교양필수");
    }

    @Test
    void courseType을_여러_개_지정하면_OR로_정렬되어_조회한다() {
        List<GraduationRule> result =
                graduationRuleRepository.search(null, List.of(CourseType.FIRST_MAJOR, CourseType.LIBERAL_ARTS));

        assertThat(result).extracting(GraduationRule::getRuleName).containsExactly("전공영역", "교양필수");
    }

    @Test
    void 규칙종류와_이름으로_단건_조회한다() {
        var found = graduationRuleRepository.findByRuleTypeIdAndRuleName(generalTypeId, "총학점");

        assertThat(found).isPresent();
        assertThat(found.get().getRuleConfig()).isEqualTo("{}");
    }
}
