package com.donggree.curriculum.internal.domain;

import com.donggree.curriculum.RuleCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 졸업 규칙의 종류를 나타내는 엔티티.
 * type_name은 graduation 모듈의 evaluator 클래스 분기 식별자로 사용한다.
 * category로 해당 규칙이 교양/전공/졸업요건 중 어느 부문인지 구분한다.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleCategory category;

    @Column(length = 255)
    private String description;

    private RuleType(String typeName, RuleCategory category, String description) {
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        this.typeName = typeName;
        this.category = category;
        this.description = description;
    }

    public static RuleType create(String typeName, RuleCategory category, String description) {
        return new RuleType(typeName, category, description);
    }
}
