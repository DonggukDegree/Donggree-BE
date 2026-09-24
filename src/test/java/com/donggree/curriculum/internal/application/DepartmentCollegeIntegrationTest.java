package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.internal.application.command.RequirementSetCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.enums.RequirementTrack;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAuditingConfig.class, RequirementSetCommandService.class, CurriculumLookupServiceImpl.class})
class DepartmentCollegeIntegrationTest {
    @Autowired
    private RequirementSetCommandService commandService;

    @Autowired
    private CurriculumLookupServiceImpl lookupService;

    @Autowired
    private RequirementSetRepository setRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void 소속_이동_전후의_학과와_세트를_보존하고_교육과정_적용년도로_선택한다() {
        Long oldSet = create("공과대학", 2021, 2023, RequirementTrack.GENERAL, true);
        Long newSet = create("AI융합대학", 2024, 2026, RequirementTrack.GENERAL, true);
        Long oldDept = setRepository.findById(oldSet).orElseThrow().getDepartmentId();
        Long newDept = setRepository.findById(newSet).orElseThrow().getDepartmentId();

        assertThat(oldDept).isNotEqualTo(newDept);
        assertThat(departmentRepository.findAllByDepartmentName("컴퓨터공학전공")).hasSize(2);
        // PDF에 현재 단과대가 표시되어도 교육과정 적용년도의 세트 우선
        assertThat(lookupService.findDepartmentId("컴퓨터공학전공", 2023, false, "AI융합대학"))
                .contains(oldDept);
        assertThat(lookupService.findDepartmentId("컴퓨터공학전공", 2024, false, null)).contains(newDept);
        assertThat(lookupService
                        .findActiveRequirementSet(oldDept, 2023, false)
                        .orElseThrow()
                        .id())
                .isEqualTo(oldSet);
        assertThat(lookupService
                        .findActiveRequirementSet(newDept, 2024, false)
                        .orElseThrow()
                        .id())
                .isEqualTo(newSet);
        assertThat(lookupService.findDepartmentId("컴퓨터공학전공", 2027, false, null)).isEmpty();
        assertThat(lookupService.findActiveRequirementSet(oldDept, 2027, false)).isEmpty();
        assertThat(lookupService.findActiveRequirementSet(newDept, 2027, false)).isEmpty();
    }

    @Test
    void 비활성_세트와_다른_과정은_복수전공_학과_선택에_사용하지_않는다() {
        create("공과대학", 2023, 2023, RequirementTrack.GENERAL, false);
        create("AI융합대학", 2023, 2023, RequirementTrack.ADVANCED, true);
        assertThat(lookupService.findDepartmentId("컴퓨터공학전공", 2023, false, null)).isEmpty();

        Long active = create("공과대학", 2023, 2023, RequirementTrack.GENERAL, true);
        Long departmentId = setRepository.findById(active).orElseThrow().getDepartmentId();
        assertThat(lookupService.findDepartmentId("컴퓨터공학전공", 2023, false, null)).contains(departmentId);
        assertThat(departmentRepository.findAllByDepartmentName("컴퓨터공학전공")).hasSize(2);
    }

    @Test
    void 같은_단과대_학과의_활성_세트_중복은_계속_차단한다() {
        create("공과대학", 2021, 2023, RequirementTrack.ALL, true);
        assertThatThrownBy(() -> create("공과대학", 2023, 2024, RequirementTrack.GENERAL, true))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(CurriculumErrorCode.ACTIVE_REQUIREMENT_SET_OVERLAP));
    }

    private Long create(String college, int start, int end, RequirementTrack track, boolean active) {
        return commandService.create(
                new RequirementSetCommand(college, "컴퓨터공학전공", start, end, track, "통합 테스트", null, active, List.of()));
    }
}
