package com.donggree.transcript.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GradingSystem;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CourseGradingServiceTest {
    @Test
    void 교육과정적용년도에_관리자가_확인한_PF만_계산에서_제외한다() {
        var curriculum = mock(CurriculumLookupService.class);
        given(curriculum.findCourseClassifications(List.of("PF1001", "GR1001", "UN1001", "NO1001"), 2025))
                .willReturn(Map.of(
                        "PF1001",
                                new CourseClassificationView(
                                        CourseType.COMMON_GENERAL, "자기계발", null, null, GradingSystem.PASS_FAIL),
                        "GR1001",
                                new CourseClassificationView(
                                        CourseType.COMMON_GENERAL, "사고", null, null, GradingSystem.GRADED),
                        "UN1001",
                                new CourseClassificationView(
                                        CourseType.COMMON_GENERAL, null, null, null, GradingSystem.UNKNOWN)));
        var service = new CourseGradingService(curriculum);
        assertThat(service.passFailCodes(List.of("PF1001", "GR1001", "UN1001", "NO1001", "PF1001"), 2025))
                .containsExactly("PF1001");
        verify(curriculum).findCourseClassifications(List.of("PF1001", "GR1001", "UN1001", "NO1001"), 2025);
    }
}
