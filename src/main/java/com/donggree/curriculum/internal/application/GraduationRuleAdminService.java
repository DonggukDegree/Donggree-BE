package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.dto.GraduationRuleCommand;
import com.donggree.curriculum.internal.application.dto.GraduationRuleResponse;
import com.donggree.curriculum.internal.application.dto.GraduationRuleUpsertCommand;
import com.donggree.curriculum.internal.application.dto.RuleTypeResponse;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.GraduationRule;
import com.donggree.curriculum.internal.domain.GraduationRuleRepository;
import com.donggree.curriculum.internal.domain.RuleType;
import com.donggree.curriculum.internal.domain.RuleTypeRepository;
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
 * 관리자 대시보드의 졸업 규칙(graduation_rule)·규칙 종류(rule_type) 관리를 담당하는 응용 서비스.
 * 트랜잭션 경계와 도메인 조립을 담당하고, 비즈니스 규칙은 도메인(GraduationRule)에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class GraduationRuleAdminService {

    private final GraduationRuleRepository graduationRuleRepository;
    private final RuleTypeRepository ruleTypeRepository;

    /** 규칙 종류 전체를 조회한다. 졸업 규칙 필터의 선택지로 사용된다. */
    @Transactional(readOnly = true)
    public List<RuleTypeResponse> getRuleTypes() {
        return ruleTypeRepository.findAll().stream()
                .map(rt -> new RuleTypeResponse(rt.getId(), rt.getTypeName(), rt.getCourseType(), rt.getDescription()))
                .toList();
    }

    /**
     * 동적 다중 필터로 졸업 규칙을 조회한다. rule_type 정보(typeName, courseType)를 함께 채워 반환한다.
     * 정렬은 course_type, rule_type_id 순(리포지토리에서 처리). 각 필터 목록이 비어 있으면 전체를 반환한다.
     */
    @Transactional(readOnly = true)
    public List<GraduationRuleResponse> search(List<Long> ruleTypeIds, List<CourseType> courseTypes) {
        List<GraduationRule> rows = graduationRuleRepository.search(ruleTypeIds, courseTypes);
        Map<Long, RuleType> ruleTypeById = loadRuleTypes(rows);
        return rows.stream()
                .map(rule -> toResponse(rule, ruleTypeById.get(rule.getRuleTypeId())))
                .toList();
    }

    /**
     * 졸업 규칙을 배치로 업서트한다. 항목의 id가 null이면 신규 등록, non-null이면 전체 교체(수정)한다.
     * 전체가 하나의 트랜잭션으로 처리되어 일부라도 실패하면 모두 롤백된다.
     *
     * @return 각 항목의 결과 ID 목록(입력 순서와 동일)
     */
    @Transactional
    public List<Long> upsert(List<GraduationRuleUpsertCommand> items) {
        validateNoDuplicateKeysInBatch(items);
        validateRuleTypesExist(items);

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
    private void validateRuleTypesExist(List<GraduationRuleUpsertCommand> items) {
        Set<Long> ruleTypeIds =
                items.stream().map(item -> item.data().ruleTypeId()).collect(Collectors.toSet());
        if (ruleTypeIds.isEmpty()) {
            return;
        }
        int foundCount = ruleTypeRepository.findAllById(ruleTypeIds).size();
        if (foundCount != ruleTypeIds.size()) {
            throw new GeneralException(CurriculumErrorCode.RULE_TYPE_NOT_FOUND);
        }
    }

    private Map<Long, RuleType> loadRuleTypes(List<GraduationRule> rows) {
        List<Long> ruleTypeIds =
                rows.stream().map(GraduationRule::getRuleTypeId).distinct().toList();
        if (ruleTypeIds.isEmpty()) {
            return Map.of();
        }
        return ruleTypeRepository.findAllById(ruleTypeIds).stream()
                .collect(Collectors.toMap(RuleType::getId, rt -> rt));
    }

    private GraduationRuleResponse toResponse(GraduationRule rule, RuleType ruleType) {
        return new GraduationRuleResponse(
                rule.getId(),
                rule.getRuleTypeId(),
                ruleType == null ? null : ruleType.getTypeName(),
                ruleType == null ? null : ruleType.getCourseType(),
                rule.getRuleName(),
                rule.getRuleConfig(),
                rule.getDescription());
    }
}
