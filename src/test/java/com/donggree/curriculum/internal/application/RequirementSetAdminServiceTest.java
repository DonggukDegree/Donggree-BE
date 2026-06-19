package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.internal.application.dto.RequirementSetCommand;
import com.donggree.curriculum.internal.application.dto.RequirementSetResponse;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.Department;
import com.donggree.curriculum.internal.domain.DepartmentRepository;
import com.donggree.curriculum.internal.domain.GraduationRule;
import com.donggree.curriculum.internal.domain.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.RequirementSet;
import com.donggree.curriculum.internal.domain.RequirementSetRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class RequirementSetAdminServiceTest {

    private final RequirementSetRepository requirementSetRepository = Mockito.mock(RequirementSetRepository.class);
    private final GraduationRuleRepository graduationRuleRepository = Mockito.mock(GraduationRuleRepository.class);
    private final DepartmentRepository departmentRepository = Mockito.mock(DepartmentRepository.class);
    private final RequirementSetAdminService service =
            new RequirementSetAdminService(requirementSetRepository, graduationRuleRepository, departmentRepository);

    private RequirementSet set(Long id) {
        RequirementSet s = RequirementSet.create(1L, 2023, 2025, 1, "설명", null, true);
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

    private RequirementSetCommand command(List<Long> ruleIds) {
        return new RequirementSetCommand(1L, 2023, 2025, 1, "설명", null, true, ruleIds);
    }

    @Test
    void 생성_시_규칙을_연결하고_생성된_ID를_반환한다() {
        given(departmentRepository.existsById(1L)).willReturn(true);
        given(requirementSetRepository.findByDepartmentIdAndYearStartAndYearEndAndVersion(1L, 2023, 2025, 1))
                .willReturn(Optional.empty());
        given(graduationRuleRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(rule(10L), rule(20L)));
        given(requirementSetRepository.save(any())).willAnswer(invocation -> {
            RequirementSet saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long id = service.create(command(List.of(10L, 20L)));

        assertThat(id).isEqualTo(100L);
    }

    @Test
    void 생성_시_존재하지_않는_학과면_예외를_던진다() {
        given(departmentRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> service.create(command(List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DEPARTMENT_NOT_FOUND));
    }

    @Test
    void 생성_시_동일_학과_연도_버전이_있으면_예외를_던진다() {
        given(departmentRepository.existsById(1L)).willReturn(true);
        given(requirementSetRepository.findByDepartmentIdAndYearStartAndYearEndAndVersion(1L, 2023, 2025, 1))
                .willReturn(Optional.of(set(1L)));

        assertThatThrownBy(() -> service.create(command(List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DUPLICATE_REQUIREMENT_SET));
    }

    @Test
    void 생성_시_존재하지_않는_규칙ID가_있으면_예외를_던진다() {
        given(departmentRepository.existsById(1L)).willReturn(true);
        given(requirementSetRepository.findByDepartmentIdAndYearStartAndYearEndAndVersion(1L, 2023, 2025, 1))
                .willReturn(Optional.empty());
        given(graduationRuleRepository.findAllById(List.of(10L, 999L))).willReturn(List.of(rule(10L)));

        assertThatThrownBy(() -> service.create(command(List.of(10L, 999L))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND));
    }

    @Test
    void 수정_시_존재하지_않는_세트면_예외를_던진다() {
        given(requirementSetRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, command(List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));
    }

    @Test
    void 수정_시_스칼라와_연결규칙이_교체된다() {
        RequirementSet existing = set(1L);
        existing.addRule(rule(99L));
        given(requirementSetRepository.findById(1L)).willReturn(Optional.of(existing));
        given(departmentRepository.existsById(2L)).willReturn(true);
        given(requirementSetRepository.findByDepartmentIdAndYearStartAndYearEndAndVersion(2L, 2024, 2026, 2))
                .willReturn(Optional.empty());
        given(graduationRuleRepository.findAllById(List.of(10L))).willReturn(List.of(rule(10L)));

        service.update(1L, new RequirementSetCommand(2L, 2024, 2026, 2, "수정", "url", false, List.of(10L)));

        assertThat(existing.getDepartmentId()).isEqualTo(2L);
        assertThat(existing.getYearStart()).isEqualTo(2024);
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getRules()).extracting(GraduationRule::getId).containsExactly(10L);
    }

    @Test
    void 단건_조회_시_연결규칙_ID와_학과명을_채워_반환한다() {
        RequirementSet existing = set(1L);
        existing.replaceRules(List.of(rule(10L), rule(20L)));
        given(requirementSetRepository.findById(1L)).willReturn(Optional.of(existing));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(Department.create("공과대학", "컴퓨터·AI학부")));

        RequirementSetResponse response = service.get(1L);

        assertThat(response.departmentName()).isEqualTo("컴퓨터·AI학부");
        assertThat(response.graduationRuleIds()).containsExactly(10L, 20L);
    }
}
