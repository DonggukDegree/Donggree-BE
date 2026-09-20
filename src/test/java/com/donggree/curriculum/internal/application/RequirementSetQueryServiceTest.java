package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.internal.application.projection.RequirementSetProjection;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepositoryCustom;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class RequirementSetQueryServiceTest {

    private final RequirementSetRepositoryCustom requirementSetQueryRepository =
            Mockito.mock(RequirementSetRepositoryCustom.class);
    private final RequirementSetRepository requirementSetRepository = Mockito.mock(RequirementSetRepository.class);
    private final DepartmentRepository departmentRepository = Mockito.mock(DepartmentRepository.class);
    private final RequirementSetQueryService service = new RequirementSetQueryService(
            requirementSetQueryRepository, requirementSetRepository, departmentRepository);

    private RequirementSet set(Long id, int yearStart, int yearEnd, int version, boolean active) {
        RequirementSet s =
                RequirementSet.create(1L, yearStart, yearEnd, RequirementTrack.ALL, version, "설명", null, active);
        if (id != null) {
            ReflectionTestUtils.setField(s, "id", id);
        }
        return s;
    }

    private GraduationRule rule(Long id) {
        GraduationRule r = GraduationRule.create(1L, "규칙" + id, "{}", null);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private Department department(Long id, Long collegeId, String departmentName) {
        Department d = Department.create(collegeId, departmentName);
        if (id != null) {
            ReflectionTestUtils.setField(d, "id", id);
        }
        return d;
    }

    @Test
    void 단건_조회_시_연결규칙_ID와_학과명을_채워_반환한다() {
        RequirementSet existing = set(1L, 2023, 2025, 1, true);
        existing.replaceRules(List.of(rule(10L), rule(20L)));
        given(requirementSetRepository.findById(1L)).willReturn(Optional.of(existing));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department(1L, 1L, "컴퓨터·AI학부")));

        RequirementSetProjection response = service.get(1L);

        assertThat(response.departmentName()).isEqualTo("컴퓨터·AI학부");
        assertThat(response.graduationRuleIds()).containsExactly(10L, 20L);
    }
}
