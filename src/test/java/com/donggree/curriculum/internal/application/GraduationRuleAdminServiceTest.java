package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.dto.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.dto.GraduationRuleResponse;
import com.donggree.curriculum.internal.application.dto.GraduationRuleUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.GraduationRule;
import com.donggree.curriculum.internal.domain.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.RuleType;
import com.donggree.curriculum.internal.domain.RuleTypeRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class GraduationRuleAdminServiceTest {

    private final GraduationRuleRepository graduationRuleRepository = Mockito.mock(GraduationRuleRepository.class);
    private final RuleTypeRepository ruleTypeRepository = Mockito.mock(RuleTypeRepository.class);
    private final GraduationRuleAdminService service =
            new GraduationRuleAdminService(graduationRuleRepository, ruleTypeRepository);

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
    void 규칙종류_전체를_조회한다() {
        given(ruleTypeRepository.findAll())
                .willReturn(List.of(
                        ruleType(1L, "TOTAL_CREDITS", null), ruleType(2L, "MIN_AREA_CREDITS", CourseType.FIRST_MAJOR)));

        var result = service.getRuleTypes();

        assertThat(result).extracting(r -> r.typeName()).containsExactly("TOTAL_CREDITS", "MIN_AREA_CREDITS");
    }

    @Test
    void 조회_시_rule_type_정보를_채워_반환한다() {
        GraduationRule r = rule(1L, 10L, "전공영역");
        given(graduationRuleRepository.search(null, null)).willReturn(List.of(r));
        given(ruleTypeRepository.findAllById(List.of(10L)))
                .willReturn(List.of(ruleType(10L, "MIN_AREA_CREDITS", CourseType.FIRST_MAJOR)));

        List<GraduationRuleResponse> result = service.search(null, null);

        assertThat(result).hasSize(1);
        GraduationRuleResponse response = result.get(0);
        assertThat(response.ruleTypeId()).isEqualTo(10L);
        assertThat(response.typeName()).isEqualTo("MIN_AREA_CREDITS");
        assertThat(response.courseType()).isEqualTo(CourseType.FIRST_MAJOR);
        assertThat(response.ruleName()).isEqualTo("전공영역");
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
}
