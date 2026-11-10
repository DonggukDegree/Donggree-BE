package com.donggree.graduation.internal.infrastructure.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class JacksonReportSettingsReaderTest {
    private final JacksonReportSettingsReader reader = new JacksonReportSettingsReader();

    @Test
    void 표시_학점은_누락값과_문자열_숫자의_기존_처리를_보존한다() {
        assertThat(reader.intValue("{}", "minCredits", 130)).isEqualTo(130);
        assertThat(reader.intValue("{\"minCredits\":\"12\"}", "minCredits", 0)).isEqualTo(12);
        assertThat(reader.hasNumber("{\"minCredits\":\"12\"}", "minCredits")).isFalse();
        assertThat(reader.hasNumber("{\"minCredits\":12}", "minCredits")).isTrue();
        assertThat(reader.creditTarget("{\"minCredits\":\"12\"}", List.of())).isEmpty();
        assertThat(reader.intValue("invalid", "minCredits", 130)).isEqualTo(130);
    }

    @Test
    void 필수과목_세트의_중첩_학수번호를_중복_없이_읽는다() {
        assertThat(reader.requiredCodes("{\"requiredCourseSets\":[[[\"A\",\"OLD_A\"],[\"B\"]],[[\"A\"]]]}"))
                .containsExactly("A", "OLD_A", "B");
        assertThat(reader.requiredCodes("{\"courseCodes\":[\"A\",\"B\"]}")).containsExactly("A", "B");
        assertThat(reader.requiredCodes("invalid")).isEmpty();
    }

    @Test
    void 학문기초_표시_선택자는_단일값과_배열을_정규화한다() {
        assertThat(reader.creditTarget(
                                "{\"minCredits\":12,\"courseType\":\"ACADEMIC_FOUNDATION\",\"areaNames\":[\"과학\",\"수학\",\"과학\"]}",
                                List.of("courseType", "areaNames", "courseCodes"))
                        .orElseThrow()
                        .selectors())
                .containsExactly(List.of("ACADEMIC_FOUNDATION"), List.of("과학", "수학"), List.of());
    }
}
