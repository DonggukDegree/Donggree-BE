package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepositoryCustom;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * search()는 리포지토리가 QueryDSL 프로젝션으로 직접 조회하므로(엔티티 미로딩) 그 검증은
 * {@code GraduationRuleRepositoryTest}(@DataJpaTest)에서 다룬다. 여기서는 서비스 고유 로직만 검증한다.
 */
class GraduationRuleQueryServiceTest {

    private final GraduationRuleRepositoryCustom graduationRuleRepository =
            Mockito.mock(GraduationRuleRepositoryCustom.class);
    private final RuleTypeRepository ruleTypeRepository = Mockito.mock(RuleTypeRepository.class);
    private final GraduationRuleQueryService service =
            new GraduationRuleQueryService(graduationRuleRepository, ruleTypeRepository);

    private RuleType ruleType(Long id, String typeName, CourseType courseType) {
        RuleType rt = RuleType.create(typeName, courseType, null);
        ReflectionTestUtils.setField(rt, "id", id);
        return rt;
    }

    @Test
    void 규칙종류_전체를_조회한다() {
        given(ruleTypeRepository.findAll())
                .willReturn(List.of(
                        ruleType(1L, "TOTAL_CREDITS", null), ruleType(2L, "MIN_AREA_CREDITS", CourseType.FIRST_MAJOR)));

        var result = service.getRuleTypes();

        assertThat(result).extracting(r -> r.typeName()).containsExactly("TOTAL_CREDITS", "MIN_AREA_CREDITS");
    }
}
