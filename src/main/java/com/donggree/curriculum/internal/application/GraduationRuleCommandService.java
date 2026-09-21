package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.command.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.command.GraduationRuleUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleConfigValidator;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 졸업 규칙(graduation_rule) 변경(Command) 응용 서비스.
 * 트랜잭션 경계와 도메인 조립을 담당하고, 비즈니스 규칙은 도메인(GraduationRule)에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class GraduationRuleCommandService {

    private final GraduationRuleRepository graduationRuleRepository;
    private final RuleTypeRepository ruleTypeRepository;

    /**
     * 졸업 규칙을 배치로 업서트한다. 항목의 id가 null이면 신규 등록, non-null이면 전체 교체(수정)한다.
     * 전체가 하나의 트랜잭션으로 처리되어 일부라도 실패하면 모두 롤백된다.
     *
     * @return 각 항목의 결과 ID 목록(입력 순서와 동일)
     */
    @Transactional
    public List<Long> upsert(List<GraduationRuleUpsertCommand> items) {
        validateNoDuplicateKeysInBatch(items);
        Map<Long, RuleType> ruleTypes = findAndValidateRuleTypes(items);
        validateRuleConfigs(items, ruleTypes);

        List<Long> ids = new ArrayList<>();
        for (GraduationRuleUpsertCommand item : items) {
            ids.add(item.id() == null ? create(item.data()) : update(item.id(), item.data()));
        }
        return ids;
    }

    private Long create(GraduationRuleCommand command) {
        graduationRuleRepository
                .findByRuleTypeIdAndRuleName(command.ruleTypeId(), command.ruleName())
                .ifPresent(existing -> {
                    throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
                });

        GraduationRule saved = graduationRuleRepository.save(GraduationRule.create(
                command.ruleTypeId(), command.ruleName(), command.ruleConfig(), command.description()));
        return saved.getId();
    }

    private Long update(Long id, GraduationRuleCommand command) {
        GraduationRule rule = graduationRuleRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND));

        graduationRuleRepository
                .findByRuleTypeIdAndRuleName(command.ruleTypeId(), command.ruleName())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
                });

        rule.update(command.ruleTypeId(), command.ruleName(), command.ruleConfig(), command.description());
        return id;
    }

    /** 한 배치 안에 (ruleTypeId, ruleName)가 중복된 항목이 있으면 예외를 던진다. */
    private void validateNoDuplicateKeysInBatch(List<GraduationRuleUpsertCommand> items) {
        Set<String> keys = new HashSet<>();
        for (GraduationRuleUpsertCommand item : items) {
            GraduationRuleCommand data = item.data();
            String key = data.ruleTypeId() + "|" + data.ruleName();
            if (!keys.add(key)) {
                throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
            }
        }
    }

    /** 배치에 등장하는 모든 ruleTypeId(중복 제외)가 실제로 존재하는지 한 번의 조회로 검증한다. */
    private Map<Long, RuleType> findAndValidateRuleTypes(List<GraduationRuleUpsertCommand> items) {
        Set<Long> ruleTypeIds =
                items.stream().map(item -> item.data().ruleTypeId()).collect(Collectors.toSet());
        if (ruleTypeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RuleType> ruleTypes = ruleTypeRepository.findAllById(ruleTypeIds).stream()
                .collect(Collectors.toMap(RuleType::getId, ruleType -> ruleType));
        if (ruleTypes.size() != ruleTypeIds.size()) {
            throw new GeneralException(CurriculumErrorCode.RULE_TYPE_NOT_FOUND);
        }
        return ruleTypes;
    }

    private void validateRuleConfigs(List<GraduationRuleUpsertCommand> items, Map<Long, RuleType> ruleTypes) {
        for (GraduationRuleUpsertCommand item : items) {
            GraduationRuleCommand data = item.data();
            RuleType ruleType = ruleTypes.get(data.ruleTypeId());
            if (!GraduationRuleConfigValidator.hasValidMajorRoles(ruleType.getTypeName(), data.ruleConfig())) {
                throw new GeneralException(CurriculumErrorCode.INVALID_MAJOR_ROLE_CONFIG);
            }
        }
    }
}
