package com.donggree.curriculum.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 개별 졸업 규칙을 나타내는 엔티티.
 * RequirementSet 애그리거트의 하위 엔티티로, 반드시 RequirementSet을 통해 생성된다.
 * ruleConfig에 JSON 형태의 규칙 설정을 저장하며, graduation 모듈에서 파싱하여 사용한다.
 */
@Entity
@Table(name = "graduation_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GraduationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_set_id")
    private RequirementSet requirementSet;

    @Column(name = "rule_type_id", nullable = false)
    private Long ruleTypeId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_config", nullable = false, columnDefinition = "jsonb")
    private String ruleConfig;

    @Column(length = 255)
    private String description;

    private GraduationRule(Long ruleTypeId, String ruleName, String ruleConfig, String description) {
        if (ruleTypeId == null) {
            throw new IllegalArgumentException("ruleTypeId must not be null");
        }
        this.ruleTypeId = ruleTypeId;
        this.ruleName = ruleName;
        this.ruleConfig = ruleConfig;
        this.description = description;
    }

    static GraduationRule create(Long ruleTypeId, String ruleName, String ruleConfig, String description) {
        return new GraduationRule(ruleTypeId, ruleName, ruleConfig, description);
    }

    void assignRequirementSet(RequirementSet requirementSet) {
        this.requirementSet = requirementSet;
    }
}
