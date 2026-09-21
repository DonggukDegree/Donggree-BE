package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.command.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.command.GraduationRuleUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class GraduationRuleCommandServiceTest {

    private final GraduationRuleRepository graduationRuleRepository = Mockito.mock(GraduationRuleRepository.class);
    private final RuleTypeRepository ruleTypeRepository = Mockito.mock(RuleTypeRepository.class);
    private final GraduationRuleCommandService service =
            new GraduationRuleCommandService(graduationRuleRepository, ruleTypeRepository);

    private GraduationRule rule(Long id, Long ruleTypeId, String ruleName) {
        GraduationRule r = GraduationRule.create(ruleTypeId, ruleName, "{}", null);
        if (id != null) {
            ReflectionTestUtils.setField(r, "id", id);
        }
        return r;
    }

    private RuleType ruleType(Long id, String typeName, CourseType courseType) {
        RuleType rt = RuleType.create(typeName, courseType, null);
        ReflectionTestUtils.setField(rt, "id", id);
        return rt;
    }

    private GraduationRuleUpsertCommand item(Long id, GraduationRuleCommand data) {
        return new GraduationRuleUpsertCommand(id, data);
    }

    @Test
    void 배치_업서트_시_id가_null이면_등록하고_생성된_ID를_반환한다() {
        GraduationRuleCommand data = new GraduationRuleCommand(10L, "총학점", "{\"min\":130}", null);
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        given(graduationRuleRepository.findByRuleTypeIdAndRuleName(10L, "총학점")).willReturn(Optional.empty());
        given(graduationRuleRepository.save(any())).willAnswer(invocation -> {
            GraduationRule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        List<Long> ids = service.upsert(List.of(item(null, data)));

        assertThat(ids).containsExactly(100L);
    }

    @Test
    void 배치_업서트_시_id가_있으면_수정하고_도메인이_전체_교체된다() {
        GraduationRule existing = rule(1L, 10L, "옛이름");
        given(graduationRuleRepository.findById(1L)).willReturn(Optional.of(existing));
        given(ruleTypeRepository.findAllById(Set.of(20L)))
                .willReturn(List.of(ruleType(20L, "MIN_AREA_CREDITS", CourseType.FIRST_MAJOR)));
        given(graduationRuleRepository.findByRuleTypeIdAndRuleName(20L, "새이름")).willReturn(Optional.empty());

        List<Long> ids = service.upsert(List.of(item(1L, new GraduationRuleCommand(20L, "새이름", "{\"x\":1}", "설명"))));

        assertThat(ids).containsExactly(1L);
        assertThat(existing.getRuleTypeId()).isEqualTo(20L);
        assertThat(existing.getRuleName()).isEqualTo("새이름");
        assertThat(existing.getRuleConfig()).isEqualTo("{\"x\":1}");
    }

    @Test
    void 배치_업서트_등록_시_기존과_동일_종류_이름이_있으면_예외를_던진다() {
        GraduationRuleCommand data = new GraduationRuleCommand(10L, "총학점", "{}", null);
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        given(graduationRuleRepository.findByRuleTypeIdAndRuleName(10L, "총학점"))
                .willReturn(Optional.of(rule(1L, 10L, "총학점")));

        assertThatThrownBy(() -> service.upsert(List.of(item(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE));
    }

    @Test
    void 배치_업서트_등록_시_존재하지_않는_ruleTypeId면_예외를_던진다() {
        GraduationRuleCommand data = new GraduationRuleCommand(999L, "총학점", "{}", null);
        given(ruleTypeRepository.findAllById(Set.of(999L))).willReturn(List.of());

        assertThatThrownBy(() -> service.upsert(List.of(item(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.RULE_TYPE_NOT_FOUND));
    }

    @Test
    void 배치_업서트_수정_시_존재하지_않는_규칙이면_예외를_던진다() {
        GraduationRuleCommand data = new GraduationRuleCommand(10L, "총학점", "{}", null);
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        given(graduationRuleRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(List.of(item(404L, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND));
    }

    @Test
    void 배치_안에_동일_종류_이름_항목이_중복되면_예외를_던진다() {
        GraduationRuleCommand data = new GraduationRuleCommand(10L, "총학점", "{}", null);

        assertThatThrownBy(() -> service.upsert(List.of(item(null, data), item(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE));
    }

    @Test
    void 적용_대상이_없는_MIN_CREDITS는_저장할_수_없다() {
        GraduationRuleCommand data =
                new GraduationRuleCommand(10L, "전공 72학점", "{\"courseType\":\"FIRST_MAJOR\",\"minCredits\":72}", null);
        given(ruleTypeRepository.findAllById(Set.of(10L)))
                .willReturn(List.of(ruleType(10L, "MIN_CREDITS", CourseType.FIRST_MAJOR)));

        assertThatThrownBy(() -> service.upsert(List.of(item(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.INVALID_MAJOR_ROLE_CONFIG));
    }

    @Test
    void 적용_대상이_있는_REQUIRED_COURSE는_저장할_수_있다() {
        GraduationRuleCommand data = new GraduationRuleCommand(
                10L, "자료구조 필수", "{\"courseCodes\":[\"CSE2001\"],\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}", null);
        given(ruleTypeRepository.findAllById(Set.of(10L)))
                .willReturn(List.of(ruleType(10L, "REQUIRED_COURSE", CourseType.FIRST_MAJOR)));
        given(graduationRuleRepository.findByRuleTypeIdAndRuleName(10L, "자료구조 필수"))
                .willReturn(Optional.empty());
        given(graduationRuleRepository.save(any())).willAnswer(invocation -> {
            GraduationRule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        assertThat(service.upsert(List.of(item(null, data)))).containsExactly(100L);
    }

    @Test
    void THESIS_등록시_적용_대상_누락을_거절한다() {
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "THESIS", null)));
        var data = new GraduationRuleCommand(10L, "졸업시험 합격", "{}", null);

        assertThatThrownBy(() -> service.upsert(List.of(item(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.INVALID_MAJOR_ROLE_CONFIG));
    }

    @Test
    void 기존_THESIS에_복수전공_적용_대상을_저장한다() {
        GraduationRule existing = rule(1L, 10L, "졸업시험 합격");
        given(graduationRuleRepository.findById(1L)).willReturn(Optional.of(existing));
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "THESIS", null)));
        given(graduationRuleRepository.findByRuleTypeIdAndRuleName(10L, "졸업시험 합격"))
                .willReturn(Optional.of(existing));
        String config = "{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]}";

        assertThat(service.upsert(List.of(item(1L, new GraduationRuleCommand(10L, "졸업시험 합격", config, null)))))
                .containsExactly(1L);
        assertThat(existing.getRuleConfig()).isEqualTo(config);
    }

    @Test
    void 영어강의_등록시_적용_대상_누락을_거절한다() {
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "ENGLISH_COURSE", null)));
        var data = new GraduationRuleCommand(
                10L, "전공 영어강의 4과목", "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":4}", null);

        assertThatThrownBy(() -> service.upsert(List.of(item(null, data))))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(CurriculumErrorCode.INVALID_MAJOR_ROLE_CONFIG));
    }

    @Test
    void 기존_영어강의에_복수전공_적용_대상을_저장한다() {
        GraduationRule existing = rule(1L, 10L, "전공 영어강의 4과목");
        given(graduationRuleRepository.findById(1L)).willReturn(Optional.of(existing));
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "ENGLISH_COURSE", null)));
        given(graduationRuleRepository.findByRuleTypeIdAndRuleName(10L, "전공 영어강의 4과목"))
                .willReturn(Optional.of(existing));
        String config = "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":4,\"applicableMajorRoles\":[\"SECONDARY\"]}";

        assertThat(service.upsert(List.of(item(1L, new GraduationRuleCommand(10L, "전공 영어강의 4과목", config, null)))))
                .containsExactly(1L);
        assertThat(existing.getRuleConfig()).isEqualTo(config);
    }
}
