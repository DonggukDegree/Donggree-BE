package com.donggree.curriculum.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 졸업 규칙의 종류를 나타내는 엔티티.
 * 규칙 유형명과 설명을 관리한다. (예: 최소학점, 필수과목 등)
 */
@Entity
@Table(name = "rule_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;

    @Column(length = 255)
    private String description;

    private RuleType(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    public static RuleType create(String typeName, String description) {
        return new RuleType(typeName, description);
    }
}
