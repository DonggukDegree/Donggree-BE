package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.command.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.command.GraduationRuleUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.application.graduationrule.GraduationRuleCommandService;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleConfig;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({GraduationRuleCommandService.class, JpaAuditingConfig.class})
class GraduationRuleCommandIntegrationTest {

    @Autowired
    private GraduationRuleCommandService service;

    @Autowired
    private GraduationRuleRepository ruleRepository;

    @Autowired
    private RuleTypeRepository typeRepository;

    @Test
    void 그룹_규칙을_저장하고_충족_수를_수정하되_겹치는_그룹으로는_교체하지_않는다() {
        Long typeId = typeRepository
                .save(RuleType.create("MIN_CREDITS", CourseType.FIRST_MAJOR, null))
                .getId();
        String config =
                """
                {"creditGroups":[{"courseCodes":["ABC*"],"minCredits":27},
                                 {"courseCodes":["DEF*"],"minCredits":27}],"minSatisfiedGroups":1,
                 "applicableMajorRoles":["SINGLE_PRIMARY"]}
                """;
        Long id = service.upsert(List.of(new GraduationRuleUpsertCommand(
                        null, new GraduationRuleCommand(typeId, "계열별 이수", config, null))))
                .getFirst();
        String updated = config.replace("\"minSatisfiedGroups\":1", "\"minSatisfiedGroups\":2");
        service.upsert(List.of(
                new GraduationRuleUpsertCommand(id, new GraduationRuleCommand(typeId, "계열별 이수", updated, null))));
        assertThat(ruleRepository.findById(id).orElseThrow().getRuleConfig())
                .isEqualTo(GraduationRuleConfig.normalize(updated));
        assertThatThrownBy(() -> service.upsert(List.of(new GraduationRuleUpsertCommand(
                        id, new GraduationRuleCommand(typeId, "계열별 이수", updated.replace("DEF*", "ABC1*"), null)))))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(CurriculumErrorCode.INVALID_GRADUATION_RULE_CONFIG));
        assertThat(ruleRepository.findById(id).orElseThrow().getRuleConfig())
                .isEqualTo(GraduationRuleConfig.normalize(updated));
    }

    @Test
    void 같은_이름의_역할별_규칙을_저장하고_자기_설명만_수정할_수_있다() {
        Long typeId = typeRepository
                .save(RuleType.create("MIN_CREDITS", CourseType.FIRST_MAJOR, null))
                .getId();
        String primary = "{\"minCredits\":72,\"applicableMajorRoles\":[\"SINGLE_PRIMARY\"]}";
        String secondary = "{\"minCredits\":36,\"applicableMajorRoles\":[\"SECONDARY\"]}";
        List<Long> ids = service.upsert(List.of(
                new GraduationRuleUpsertCommand(null, new GraduationRuleCommand(typeId, "전공 최저", primary, null)),
                new GraduationRuleUpsertCommand(null, new GraduationRuleCommand(typeId, "전공 최저", secondary, null))));

        service.upsert(List.of(new GraduationRuleUpsertCommand(
                ids.get(0), new GraduationRuleCommand(typeId, "전공 최저", primary, "설명 수정"))));

        assertThat(ruleRepository.findAllByRuleTypeIdAndRuleName(typeId, "전공 최저"))
                .hasSize(2);
        assertThat(ruleRepository.findById(ids.get(0)).orElseThrow().getDescription())
                .isEqualTo("설명 수정");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 배치_뒤쪽의_기존_규칙_중복이면_앞에서_저장한_규칙까지_롤백한다() {
        // 테스트 트랜잭션 대신 실제 서비스 트랜잭션의 롤백 경계를 검증한다.
        Long typeId = typeRepository
                .save(RuleType.create("TOTAL_CREDITS", null, null))
                .getId();
        Long existingId = ruleRepository
                .save(GraduationRule.create(typeId, "기존 규칙", "{}", null))
                .getId();
        try {
            assertThatThrownBy(() -> service.upsert(List.of(
                            new GraduationRuleUpsertCommand(
                                    null, new GraduationRuleCommand(typeId, "신규 규칙", "{}", null)),
                            new GraduationRuleUpsertCommand(
                                    null, new GraduationRuleCommand(typeId, "기존 규칙", "{}", "설명만 다름")))))
                    .isInstanceOf(GeneralException.class);

            assertThat(ruleRepository.findAllByRuleTypeIdAndRuleName(typeId, "신규 규칙"))
                    .isEmpty();
            assertThat(ruleRepository.findById(existingId)).isPresent();
        } finally {
            ruleRepository.deleteById(existingId);
            typeRepository.deleteById(typeId);
        }
    }

    @Test
    void 실제_저장소에_있는_미지원_규칙_종류도_저장_전에_거절한다() {
        Long typeId =
                typeRepository.save(RuleType.create("UNSUPPORTED", null, null)).getId();
        assertThatThrownBy(() -> service.upsert(List.of(new GraduationRuleUpsertCommand(
                        null, new GraduationRuleCommand(typeId, "미지원 규칙", "{}", null)))))
                .isInstanceOfSatisfying(GeneralException.class, error -> assertThat(error.getCode())
                        .isEqualTo(CurriculumErrorCode.UNSUPPORTED_RULE_TYPE));
        assertThat(ruleRepository.findAllByRuleTypeIdAndRuleName(typeId, "미지원 규칙"))
                .isEmpty();
    }
}
