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
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleConfig;
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
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "총학점"))
                .willReturn(List.of());
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
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(20L, "새이름"))
                .willReturn(List.of());

        List<Long> ids = service.upsert(List.of(item(1L, new GraduationRuleCommand(20L, "새이름", "{\"x\":1}", "설명"))));

        assertThat(ids).containsExactly(1L);
        assertThat(existing.getRuleTypeId()).isEqualTo(20L);
        assertThat(existing.getRuleName()).isEqualTo("새이름");
        assertThat(existing.getRuleConfig()).isEqualTo("{\"x\":1}");
    }

    @Test
    void 배치_업서트_등록_시_기존과_동일_종류_이름_옵션이면_예외를_던진다() {
        GraduationRuleCommand data = new GraduationRuleCommand(10L, "총학점", "{}", null);
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "총학점"))
                .willReturn(List.of(rule(1L, 10L, "총학점")));

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
    void 배치_안에_동일_종류_이름_옵션_항목이_중복되면_예외를_던진다() {
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
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "자료구조 필수"))
                .willReturn(List.of());
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
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "졸업시험 합격"))
                .willReturn(List.of(existing));
        String config = "{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\",\"SECONDARY\"]}";

        assertThat(service.upsert(List.of(item(1L, new GraduationRuleCommand(10L, "졸업시험 합격", config, null)))))
                .containsExactly(1L);
        assertThat(existing.getRuleConfig()).isEqualTo(GraduationRuleConfig.normalize(config));
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
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "전공 영어강의 4과목"))
                .willReturn(List.of(existing));
        String config = "{\"courseTypes\":[\"FIRST_MAJOR\"],\"minCount\":4,\"applicableMajorRoles\":[\"SECONDARY\"]}";

        assertThat(service.upsert(List.of(item(1L, new GraduationRuleCommand(10L, "전공 영어강의 4과목", config, null)))))
                .containsExactly(1L);
        assertThat(existing.getRuleConfig()).isEqualTo(GraduationRuleConfig.normalize(config));
    }

    @Test
    void 같은_이름이라도_적용_대상이_다르면_배치_등록한다() {
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "THESIS", null)));
        given(graduationRuleRepository.save(any())).willAnswer(invocation -> {
            GraduationRule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        var primary = new GraduationRuleCommand(10L, "졸업시험", "{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}", null);
        var secondary = new GraduationRuleCommand(10L, "졸업시험", "{\"applicableMajorRoles\":[\"SECONDARY\"]}", null);
        assertThat(service.upsert(List.of(item(null, primary), item(null, secondary))))
                .hasSize(2);
    }

    @Test
    void 기존과_이름이_같아도_옵션이_다르면_수정한다() {
        var current = rule(1L, 10L, "수정 전");
        var other = rule(2L, 10L, "총학점");
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        given(graduationRuleRepository.findById(1L)).willReturn(Optional.of(current));
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "총학점"))
                .willReturn(List.of(other));
        assertThat(service.upsert(
                        List.of(item(1L, new GraduationRuleCommand(10L, "총학점", "{\"minCredits\":130}", null)))))
                .containsExactly(1L);
    }

    @Test
    void 다른_ID의_옵션까지_같아지도록_수정하면_거절한다() {
        var current = rule(1L, 10L, "수정 전");
        var other = rule(2L, 10L, "총학점");
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        given(graduationRuleRepository.findById(1L)).willReturn(Optional.of(current));
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "총학점"))
                .willReturn(List.of(other));
        assertError(
                List.of(item(1L, new GraduationRuleCommand(10L, "총학점", "{}", "다른 설명"))),
                CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
    }

    @Test
    void 역할_선택_순서와_JSON_키_순서만_다른_배치_중복을_거절한다() {
        var first = new GraduationRuleCommand(
                10L, "전공", "{\"minCredits\":36,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\"]}", null);
        var second = new GraduationRuleCommand(
                10L,
                "전공",
                "{\"applicableMajorRoles\":[\"DUAL_PRIMARY\",\"SINGLE_PRIMARY\"],\"minCredits\":36.0}",
                "다른 설명");
        assertError(List.of(item(null, first), item(null, second)), CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
    }

    @Test
    void 기존의_미정렬_JSON_옵션과도_중복을_비교한다() {
        var existing = rule(1L, 10L, "졸업시험");
        ReflectionTestUtils.setField(
                existing, "ruleConfig", "{\"applicableMajorRoles\":[\"SINGLE_PRIMARY\",\"DUAL_PRIMARY\"]}");
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "THESIS", null)));
        given(graduationRuleRepository.findAllByRuleTypeIdAndRuleName(10L, "졸업시험"))
                .willReturn(List.of(existing));
        assertError(
                List.of(item(
                        null,
                        new GraduationRuleCommand(
                                10L,
                                "졸업시험",
                                "{\"applicableMajorRoles\":[\"DUAL_PRIMARY\",\"SINGLE_PRIMARY\"]}",
                                null))),
                CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
    }

    @Test
    void 옵션이_달라도_동일_ID의_배치_중복_수정은_거절한다() {
        assertError(
                List.of(
                        item(1L, new GraduationRuleCommand(10L, "전공", "{}", null)),
                        item(1L, new GraduationRuleCommand(10L, "전공", "{\"minCredits\":36}", null))),
                CurriculumErrorCode.DUPLICATE_GRADUATION_RULE_ID);
    }

    @Test
    void 잘못된_JSON은_저장전에_400_오류로_거절한다() {
        assertError(
                List.of(item(null, new GraduationRuleCommand(10L, "총학점", "{", null))),
                CurriculumErrorCode.INVALID_GRADUATION_RULE_CONFIG);
    }

    @Test
    void 동시_요청의_DB_중복도_같은_충돌_오류로_반환한다() {
        given(ruleTypeRepository.findAllById(Set.of(10L))).willReturn(List.of(ruleType(10L, "TOTAL_CREDITS", null)));
        var sql = new java.sql.SQLException("duplicate", "23505");
        var violation = new org.hibernate.exception.ConstraintViolationException(
                "duplicate", sql, GraduationRule.UNIQUE_KEY_CONSTRAINT);
        given(graduationRuleRepository.save(any()))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate", violation));
        assertError(
                List.of(item(null, new GraduationRuleCommand(10L, "총학점", "{}", null))),
                CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
    }

    private void assertError(List<GraduationRuleUpsertCommand> items, CurriculumErrorCode expected) {
        assertThatThrownBy(() -> service.upsert(items))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode()).isEqualTo(expected));
    }
}
