package com.donggree.curriculum.internal.domain.graduationrule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 개별 졸업 규칙을 나타내는 독립 엔티티.
 * 여러 RequirementSet에서 공유될 수 있으며, RequirementSet과 N:M 관계를 갖는다.
 * ruleConfig에 JSON 형태의 규칙 설정을 저장하며, graduation 모듈에서 파싱하여 사용한다.
 */
@Entity
@Table(
        name = "graduation_rule",
        uniqueConstraints =
                @UniqueConstraint(
                        name = GraduationRule.UNIQUE_KEY_CONSTRAINT,
                        columnNames = {"rule_type_id", "rule_name", "rule_config"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GraduationRule {

    public static final String UNIQUE_KEY_CONSTRAINT = "uk_graduation_rule_type_name_config";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_type_id", nullable = false)
    private Long ruleTypeId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    // columnDefinition을 지정하지 않고 방언이 SqlTypes.JSON을 매핑하게 둔다(Postgres→jsonb, H2→json).
    // jsonb로 하드코딩하면 H2 테스트에서 DDL이 실패한다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_config", nullable = false)
    private String ruleConfig;

    @Column(length = 255)
    private String description;

    private GraduationRule(Long ruleTypeId, String ruleName, String ruleConfig, String description) {
        assignValidated(ruleTypeId, ruleName, ruleConfig, description);
    }

    public static GraduationRule create(Long ruleTypeId, String ruleName, String ruleConfig, String description) {
        return new GraduationRule(ruleTypeId, ruleName, ruleConfig, description);
    }

    /** 규칙 정보를 전체 교체한다(PUT). 생성과 동일한 불변식을 재검증한다. */
    public void update(Long ruleTypeId, String ruleName, String ruleConfig, String description) {
        assignValidated(ruleTypeId, ruleName, ruleConfig, description);
    }

    private void assignValidated(Long ruleTypeId, String ruleName, String ruleConfig, String description) {
        if (ruleTypeId == null) {
            throw new IllegalArgumentException("ruleTypeId must not be null");
        }
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("ruleName must not be null or blank");
        }
        if (ruleConfig == null || ruleConfig.isBlank()) {
            throw new IllegalArgumentException("ruleConfig must not be null or blank");
        }
        String normalizedConfig = GraduationRuleConfig.normalize(ruleConfig);
        this.ruleTypeId = ruleTypeId;
        this.ruleName = ruleName;
        this.ruleConfig = normalizedConfig;
        this.description = description;
    }
}
