package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.internal.application.command.RequirementSetCommand;
import com.donggree.curriculum.internal.application.command.RequirementSetUpdateCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.college.College;
import com.donggree.curriculum.internal.domain.college.CollegeRepository;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class RequirementSetCommandServiceTest {

    private final RequirementSetRepository requirementSetRepository = Mockito.mock(RequirementSetRepository.class);
    private final GraduationRuleRepository graduationRuleRepository = Mockito.mock(GraduationRuleRepository.class);
    private final DepartmentRepository departmentRepository = Mockito.mock(DepartmentRepository.class);
    private final CollegeRepository collegeRepository = Mockito.mock(CollegeRepository.class);
    private final RequirementSetCommandService service = new RequirementSetCommandService(
            requirementSetRepository, graduationRuleRepository, departmentRepository, collegeRepository);

    private RequirementSet set(Long id, int yearStart, int yearEnd, int version, boolean active) {
        RequirementSet s = RequirementSet.create(1L, yearStart, yearEnd, version, "설명", null, active);
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

    private College college(Long id, String collegeName) {
        College c = College.create(collegeName);
        if (id != null) {
            ReflectionTestUtils.setField(c, "id", id);
        }
        return c;
    }

    private Department department(Long id, Long collegeId, String departmentName) {
        Department d = Department.create(collegeId, departmentName);
        if (id != null) {
            ReflectionTestUtils.setField(d, "id", id);
        }
        return d;
    }

    private RequirementSetCommand command(boolean active, List<Long> ruleIds) {
        return new RequirementSetCommand("첨단융합대학", "컴퓨터·AI학부", 2023, 2025, "설명", null, active, ruleIds);
    }

    private RequirementSetUpdateCommand updateCommand(int yearStart, int yearEnd, boolean active, List<Long> ruleIds) {
        return new RequirementSetUpdateCommand(yearStart, yearEnd, "설명", null, active, ruleIds);
    }

    @Test
    void 생성_시_기존_단과대_학과를_재사용하고_다음_버전을_자동_채번해_규칙을_연결한다() {
        given(collegeRepository.findByCollegeName("첨단융합대학")).willReturn(Optional.of(college(1L, "첨단융합대학")));
        given(departmentRepository.findByDepartmentName("컴퓨터·AI학부"))
                .willReturn(Optional.of(department(1L, 1L, "컴퓨터·AI학부")));
        // 같은 학과·적용년도 lineage에 version 2가 이미 있으면 다음은 3
        given(requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(1L, 2023, 2025))
                .willReturn(Optional.of(set(50L, 2023, 2025, 2, false)));
        given(graduationRuleRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(rule(10L), rule(20L)));
        given(requirementSetRepository.save(any())).willAnswer(invocation -> {
            RequirementSet saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long id = service.create(command(true, List.of(10L, 20L)));

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<RequirementSet> captor = ArgumentCaptor.forClass(RequirementSet.class);
        Mockito.verify(requirementSetRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
    }

    @Test
    void 생성_시_단과대와_학과가_없으면_새로_등록한_뒤_첫_버전으로_세트를_만든다() {
        given(collegeRepository.findByCollegeName("첨단융합대학")).willReturn(Optional.empty());
        given(collegeRepository.save(any(College.class))).willAnswer(invocation -> {
            College saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });
        given(departmentRepository.findByDepartmentName("컴퓨터·AI학부")).willReturn(Optional.empty());
        given(departmentRepository.save(any(Department.class))).willAnswer(invocation -> {
            Department saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });
        given(requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(5L, 2023, 2025))
                .willReturn(Optional.empty());
        given(requirementSetRepository.save(any())).willAnswer(invocation -> {
            RequirementSet saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long id = service.create(command(true, List.of()));

        assertThat(id).isEqualTo(100L);
        Mockito.verify(collegeRepository).save(any(College.class));
        Mockito.verify(departmentRepository).save(any(Department.class));
        ArgumentCaptor<RequirementSet> captor = ArgumentCaptor.forClass(RequirementSet.class);
        Mockito.verify(requirementSetRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(1);
    }

    @Test
    void 활성으로_생성_시_적용년도가_겹치는_다른_활성_세트가_있으면_예외를_던진다() {
        given(collegeRepository.findByCollegeName("첨단융합대학")).willReturn(Optional.of(college(1L, "첨단융합대학")));
        given(departmentRepository.findByDepartmentName("컴퓨터·AI학부"))
                .willReturn(Optional.of(department(1L, 1L, "컴퓨터·AI학부")));
        // 기존 활성 세트가 신규 2023~2025 와 겹침 (DB exists가 true 반환)
        given(
                        requirementSetRepository
                                .existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                                        1L, 2025, 2023))
                .willReturn(true);

        assertThatThrownBy(() -> service.create(command(true, List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.ACTIVE_REQUIREMENT_SET_OVERLAP));
    }

    @Test
    void 비활성으로_생성하면_적용년도가_겹쳐도_허용한다() {
        given(collegeRepository.findByCollegeName("첨단융합대학")).willReturn(Optional.of(college(1L, "첨단융합대학")));
        given(departmentRepository.findByDepartmentName("컴퓨터·AI학부"))
                .willReturn(Optional.of(department(1L, 1L, "컴퓨터·AI학부")));
        given(requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(1L, 2023, 2025))
                .willReturn(Optional.empty());
        given(requirementSetRepository.save(any())).willAnswer(invocation -> {
            RequirementSet saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Long id = service.create(command(false, List.of()));

        assertThat(id).isEqualTo(100L);
        // 비활성은 겹침 검증을 하지 않으므로 활성 겹침 exists 쿼리를 호출하지 않는다
        Mockito.verify(requirementSetRepository, Mockito.never())
                .existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqual(
                        any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void 생성_시_존재하지_않는_규칙ID가_있으면_예외를_던진다() {
        given(collegeRepository.findByCollegeName("첨단융합대학")).willReturn(Optional.of(college(1L, "첨단융합대학")));
        given(departmentRepository.findByDepartmentName("컴퓨터·AI학부"))
                .willReturn(Optional.of(department(1L, 1L, "컴퓨터·AI학부")));
        given(requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(1L, 2023, 2025))
                .willReturn(Optional.empty());
        given(graduationRuleRepository.findAllById(List.of(10L, 999L))).willReturn(List.of(rule(10L)));

        assertThatThrownBy(() -> service.create(command(true, List.of(10L, 999L))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND));
    }

    @Test
    void 생성_시_기존_학과가_다른_단과대_소속이면_예외를_던진다() {
        // 입력 단과대는 id 2 "공과대학", 기존 학과는 단과대 id 1 소속 → 불일치
        given(collegeRepository.findByCollegeName("첨단융합대학")).willReturn(Optional.of(college(2L, "첨단융합대학")));
        given(departmentRepository.findByDepartmentName("컴퓨터·AI학부"))
                .willReturn(Optional.of(department(1L, 1L, "컴퓨터·AI학부")));

        assertThatThrownBy(() -> service.create(command(true, List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DEPARTMENT_COLLEGE_MISMATCH));
    }

    @Test
    void 수정_시_존재하지_않는_세트면_예외를_던진다() {
        given(requirementSetRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, updateCommand(2023, 2025, true, List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.REQUIREMENT_SET_NOT_FOUND));
    }

    @Test
    void 수정_시_학과는_유지하고_적용년도가_바뀌면_새_lineage의_다음_버전을_부여한다() {
        RequirementSet existing = set(1L, 2023, 2025, 3, false);
        existing.addRule(rule(99L));
        given(requirementSetRepository.findById(1L)).willReturn(Optional.of(existing));
        // 옮겨갈 2024~2026 lineage 최신 버전이 1이면 다음은 2
        given(requirementSetRepository.findTopByDepartmentIdAndYearStartAndYearEndOrderByVersionDesc(1L, 2024, 2026))
                .willReturn(Optional.of(set(8L, 2024, 2026, 1, false)));
        given(graduationRuleRepository.findAllById(List.of(10L))).willReturn(List.of(rule(10L)));

        // 수정 요청에는 학과·단과대명이 아예 없으므로 학과는 변경될 수 없다(생성 시 확정)
        service.update(1L, new RequirementSetUpdateCommand(2024, 2026, "수정", "url", false, List.of(10L)));

        assertThat(existing.getDepartmentId()).isEqualTo(1L);
        assertThat(existing.getYearStart()).isEqualTo(2024);
        assertThat(existing.getYearEnd()).isEqualTo(2026);
        assertThat(existing.getVersion()).isEqualTo(2);
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getRules()).extracting(GraduationRule::getId).containsExactly(10L);
    }

    @Test
    void 수정_시_적용년도가_그대로면_기존_버전을_유지한다() {
        RequirementSet existing = set(1L, 2023, 2025, 3, true);
        given(requirementSetRepository.findById(1L)).willReturn(Optional.of(existing));
        // 자기 자신만 활성으로 겹치므로(IdNot 제외) exists는 false → 통과
        given(graduationRuleRepository.findAllById(List.of(10L))).willReturn(List.of(rule(10L)));

        service.update(1L, updateCommand(2023, 2025, true, List.of(10L)));

        assertThat(existing.getVersion()).isEqualTo(3);
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void 활성으로_수정_시_적용년도가_겹치는_다른_활성_세트가_있으면_예외를_던진다() {
        RequirementSet existing = set(1L, 2023, 2025, 1, false);
        given(requirementSetRepository.findById(1L)).willReturn(Optional.of(existing));
        // 자신을 제외한 다른 활성 세트가 적용년도와 겹침 (DB exists가 true 반환)
        given(
                        requirementSetRepository
                                .existsByActiveTrueAndDepartmentIdAndYearStartLessThanEqualAndYearEndGreaterThanEqualAndIdNot(
                                        1L, 2025, 2023, 1L))
                .willReturn(true);

        assertThatThrownBy(() -> service.update(1L, updateCommand(2023, 2025, true, List.of())))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.ACTIVE_REQUIREMENT_SET_OVERLAP));
    }
}
