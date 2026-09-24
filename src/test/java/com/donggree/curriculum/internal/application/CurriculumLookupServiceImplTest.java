package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.RequirementSetView;
import com.donggree.curriculum.internal.domain.areatype.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.college.College;
import com.donggree.curriculum.internal.domain.college.CollegeRepository;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassificationRepository;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSet;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 요건 세트 선택 규칙에 초점을 맞춘 테스트.
 * 학과·입학년도에 더해 과정(일반/심화)까지 맞는 세트만 고르는지, 맞는 세트가 없을 때
 * 다른 과정으로 대체하지 않고 빈 결과를 돌려주는지 확인한다.
 */
class CurriculumLookupServiceImplTest {

    private static final Long DEPARTMENT_ID = 100L;

    private final RequirementSetRepository requirementSetRepository = Mockito.mock(RequirementSetRepository.class);
    private final DepartmentRepository departmentRepository = Mockito.mock(DepartmentRepository.class);
    private final CollegeRepository collegeRepository = Mockito.mock(CollegeRepository.class);

    private final CurriculumLookupServiceImpl service = new CurriculumLookupServiceImpl(
            departmentRepository,
            collegeRepository,
            requirementSetRepository,
            Mockito.mock(RuleTypeRepository.class),
            Mockito.mock(CourseClassificationRepository.class),
            Mockito.mock(AreaTypeRepository.class));

    private RequirementSet set(Long id, RequirementTrack track) {
        RequirementSet s = RequirementSet.create(DEPARTMENT_ID, 2023, 2025, track, 1, null, null, true);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private void givenActiveSets(RequirementSet... sets) {
        given(requirementSetRepository.findByDepartmentIdAndActiveTrue(DEPARTMENT_ID))
                .willReturn(List.of(sets));
    }

    @Test
    void 과정_구분이_없는_학과는_ALL_세트가_일반과정_학생에게도_심화과정_학생에게도_걸린다() {
        givenActiveSets(set(1L, RequirementTrack.ALL));

        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, false))
                .map(RequirementSetView::id)
                .contains(1L);
        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, true))
                .map(RequirementSetView::id)
                .contains(1L);
    }

    @Test
    void 일반_심화_세트가_모두_있으면_학생의_과정에_맞는_세트를_고른다() {
        givenActiveSets(set(1L, RequirementTrack.GENERAL), set(2L, RequirementTrack.ADVANCED));

        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, false))
                .map(RequirementSetView::id)
                .contains(1L);
        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, true))
                .map(RequirementSetView::id)
                .contains(2L);
    }

    @Test
    void 일반과정_세트만_있으면_심화과정_학생은_세트를_찾지_못한다() {
        // 일반과정 요건으로 대신 판정하지 않는다. 호출부(GraduationQueryService)가 미지원 학과와
        // 동일하게 REQUIREMENT_SET_NOT_FOUND로 실패시킨다.
        givenActiveSets(set(1L, RequirementTrack.GENERAL));

        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, true)).isEmpty();
        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, false)).isPresent();
    }

    @Test
    void 심화과정_세트만_있으면_일반과정_학생은_세트를_찾지_못한다() {
        givenActiveSets(set(1L, RequirementTrack.ADVANCED));

        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, false)).isEmpty();
        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, true)).isPresent();
    }

    @Test
    void 과정이_맞아도_입학년도가_적용범위_밖이면_세트를_찾지_못한다() {
        givenActiveSets(set(1L, RequirementTrack.ADVANCED));

        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2026, true)).isEmpty();
    }

    @Test
    void 활성_세트가_하나도_없으면_빈_결과를_반환한다() {
        givenActiveSets();

        assertThat(service.findActiveRequirementSet(DEPARTMENT_ID, 2023, false)).isEmpty();
    }

    private void givenNames(boolean duplicated) {
        Department old = Department.create(1L, "컴퓨터공학전공");
        ReflectionTestUtils.setField(old, "id", 10L);
        Department moved = Department.create(2L, "컴퓨터공학전공");
        ReflectionTestUtils.setField(moved, "id", 20L);
        given(departmentRepository.findAllByDepartmentName("컴퓨터공학전공"))
                .willReturn(duplicated ? List.of(old, moved) : List.of(old));
    }

    private RequirementSet departmentSet(Long dept, int start, int end, RequirementTrack track) {
        return RequirementSet.create(dept, start, end, track, 1, null, null, true);
    }

    @Test
    void 학과명이_유일하면_세트가_없어도_기존_업로드용_ID를_반환한다() {
        givenNames(false);
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, null)).contains(10L);
        Mockito.verifyNoInteractions(requirementSetRepository);
    }

    @Test
    void 동명_학과는_교육과정_적용년도에_맞는_단과대의_학과를_선택한다() {
        givenNames(true);
        given(requirementSetRepository.findByDepartmentIdInAndActiveTrue(anyList()))
                .willReturn(List.of(
                        departmentSet(10L, 2021, 2023, RequirementTrack.ALL),
                        departmentSet(20L, 2024, 2026, RequirementTrack.ALL)));
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, null)).contains(10L);
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2024, false, null)).contains(20L);
        // PDF의 현재 단과대보다 해당 교육과정 적용년도를 우선한다.
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, "AI융합대학")).contains(10L);
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2020, false, null)).isEmpty();
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2027, false, null)).isEmpty();
    }

    @Test
    void 년도가_같아도_일반과_심화_과정을_구분한다() {
        givenNames(true);
        given(requirementSetRepository.findByDepartmentIdInAndActiveTrue(anyList()))
                .willReturn(List.of(
                        departmentSet(10L, 2023, 2023, RequirementTrack.GENERAL),
                        departmentSet(20L, 2023, 2023, RequirementTrack.ADVANCED)));
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, null)).contains(10L);
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, true, null)).contains(20L);
    }

    @Test
    void 년도까지_겹치면_PDF_단과대로_구분하고_단과대가_없으면_임의_선택하지_않는다() {
        givenNames(true);
        given(requirementSetRepository.findByDepartmentIdInAndActiveTrue(anyList()))
                .willReturn(List.of(
                        departmentSet(10L, 2023, 2023, RequirementTrack.ALL),
                        departmentSet(20L, 2023, 2023, RequirementTrack.ALL)));
        College college = College.create("AI융합대학");
        ReflectionTestUtils.setField(college, "id", 2L);
        given(collegeRepository.findByCollegeName("AI융합대학")).willReturn(Optional.of(college));
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, "AI융합대학")).contains(20L);
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, null)).isEmpty();
    }

    @Test
    void 활성세트가_없으면_복수전공_학과를_임의_선택하지_않는다() {
        givenNames(true);
        given(requirementSetRepository.findByDepartmentIdInAndActiveTrue(anyList()))
                .willReturn(List.of());
        assertThat(service.findDepartmentId("컴퓨터공학전공", 2023, false, null)).isEmpty();
        assertThat(service.findDepartmentId("미등록학과", 2023, false, null)).isEmpty();
        assertThat(service.findDepartmentId(null, 2023, false, null)).isEmpty();
    }
}
