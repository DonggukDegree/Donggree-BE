package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.command.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.command.GraduationRuleUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleConfig;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleConfigValidator;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

        try {
            List<Long> ids = new ArrayList<>();
            for (GraduationRuleUpsertCommand item : items) {
                ids.add(item.id() == null ? create(item.data()) : update(item.id(), item.data()));
            }
            // 동시 요청의 유니크 충돌도 커밋 전에 잡아 동일한 409 응답으로 반환한다.
            graduationRuleRepository.flush();
            return ids;
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && violation.getConstraintName() != null
                        && violation
                                .getConstraintName()
                                .toLowerCase(Locale.ROOT)
                                .contains(GraduationRule.UNIQUE_KEY_CONSTRAINT)) {
                    throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
                }
            }
            throw ex;
        }
    }

    private Long create(GraduationRuleCommand command) {
        validateNoExistingDuplicate(command, null);

        GraduationRule saved = graduationRuleRepository.save(GraduationRule.create(
                command.ruleTypeId(), command.ruleName(), command.ruleConfig(), command.description()));
        return saved.getId();
    }

    private Long update(Long id, GraduationRuleCommand command) {
        GraduationRule rule = graduationRuleRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.GRADUATION_RULE_NOT_FOUND));

        validateNoExistingDuplicate(command, id);

        rule.update(command.ruleTypeId(), command.ruleName(), command.ruleConfig(), command.description());
        return id;
    }

    private void validateNoExistingDuplicate(GraduationRuleCommand command, Long selfId) {
        String config = normalizeConfig(command.ruleConfig());
        boolean duplicate =
                graduationRuleRepository
                        .findAllByRuleTypeIdAndRuleName(command.ruleTypeId(), command.ruleName())
                        .stream()
                        .filter(other -> !Objects.equals(other.getId(), selfId))
                        .anyMatch(other -> config.equals(GraduationRuleConfig.normalize(other.getRuleConfig())));
        if (duplicate) {
            throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
        }
    }

    /** 설명은 식별에서 제외한다. 같은 ID를 두 번 수정하는 모호한 요청도 저장 전에 거절한다. */
    private void validateNoDuplicateKeysInBatch(List<GraduationRuleUpsertCommand> items) {
        Set<RuleKey> keys = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        for (GraduationRuleUpsertCommand item : items) {
            if (item.id() != null && !ids.add(item.id())) {
                throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE_ID);
            }
            GraduationRuleCommand data = item.data();
            RuleKey key = new RuleKey(data.ruleTypeId(), data.ruleName(), normalizeConfig(data.ruleConfig()));
            if (!keys.add(key)) {
                throw new GeneralException(CurriculumErrorCode.DUPLICATE_GRADUATION_RULE);
            }
        }
    }

    private String normalizeConfig(String config) {
        try {
            return GraduationRuleConfig.normalize(config);
        } catch (IllegalArgumentException ex) {
            throw new GeneralException(CurriculumErrorCode.INVALID_GRADUATION_RULE_CONFIG);
        }
    }

    private record RuleKey(Long ruleTypeId, String ruleName, String config) {}

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
