package com.donggree.graduation.internal.infrastructure.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.graduation.internal.domain.rule.RuleConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JacksonRuleConfigurationReaderTest {
    private final JacksonRuleConfigurationReader reader = new JacksonRuleConfigurationReader();

    @ParameterizedTest
    @ValueSource(strings = {"MIN_CREDITS", "REQUIRED_COURSE", "PREREQUISITE", "THESIS", "ENGLISH_COURSE"})
    void 기존에_허용하던_역할과_추가_속성을_계속_허용한다(String typeName) {
        assertThat(reader.read(typeName, "{\"applicableMajorRoles\":[\"SECONDARY\"],\"futureSetting\":true}"))
                .isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOTAL_CREDITS", "GPA", "TEACHING_APTITUDE"})
    void 기존에_거부하던_추가_속성을_새로_허용하지_않는다(String typeName) {
        assertThatThrownBy(() -> reader.read(typeName, "{\"futureSetting\":true}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rule_config 파싱 실패");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void 빈_판정_설정을_기존과_같이_거부한다(String settings) {
        assertThatThrownBy(() -> reader.read("MIN_CREDITS", settings))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rule_config가 비어 있습니다.");
        assertThatThrownBy(() -> reader.readMajorRoles(settings)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 선택자와_기존_평점_기본값을_보존한다() {
        var config = (RuleConfiguration.MinCredits) reader.read(
                "MIN_CREDITS", "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":\"12\",\"pdfAreaNames\":[\"전문\"]}");
        assertThat(config.minCredits()).isEqualTo(12);
        assertThat(config.minCount()).isNull();
        assertThat(config.pdfAreaNames()).containsExactly("전문");
        assertThat(((RuleConfiguration.Gpa) reader.read("GPA", "{\"minGpa\":2.0}")).gpaScope())
                .isNull();
    }

    @Test
    void 역할이_배열이_아니면_기존_역할_기본값에_위임한다() {
        assertThat(reader.readMajorRoles("{\"applicableMajorRoles\":\"SECONDARY\"}"))
                .isEmpty();
        assertThat(reader.readMajorRoles("{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"SECONDARY\"]}"))
                .containsExactly("SINGLE_PRIMARY", "SECONDARY");
    }

    @Test
    void 잘못된_대체과목_행은_제외하고_읽을_수_없는_설정은_감면하지_않는다() {
        var adjustments = reader.readCreditAdjustments(
                """
                {"creditAdjustments":[
                  {"requiredCourseCodes":["REQ",null,1,""],"replacementCourseCodes":["ALT"],"credits":3},
                  {"requiredCourseCodes":[],"replacementCourseCodes":["ALT"],"credits":3},
                  {"requiredCourseCodes":["REQ2"],"replacementCourseCodes":["ALT"],"credits":0}]}
                """);
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.getFirst().requiredCourseCodes()).isEqualTo(List.of("REQ"));
        assertThat(adjustments.getFirst().credits()).isEqualTo(3);
        assertThat(reader.readCreditAdjustments("invalid")).isEmpty();
        assertThat(reader.readCreditAdjustments(null)).isEmpty();
    }

    @Test
    void 설정_없는_고정_규칙은_JSON을_새로_검증하지_않는다() {
        assertThat(reader.read("SCIENCE_CONFLICT", "invalid")).isInstanceOf(RuleConfiguration.NoSettings.class);
    }
}
