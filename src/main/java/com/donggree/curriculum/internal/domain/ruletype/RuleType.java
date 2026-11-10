package com.donggree.curriculum.internal.domain.ruletype;

import com.donggree.curriculum.CourseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 졸업 규칙의 종류를 나타내는 엔티티.
 * type_name은 graduation 모듈의 evaluator 클래스 분기 식별자로 사용한다.
 * course_type이 null이면 졸업요건 규칙(총학점, 평점 등), non-null이면 해당 영역 규칙이다.
 */
@Entity
@Table(
        name = "rule_type",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_rule_type_name_course_type",
                        columnNames = {"type_name", "course_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", length = 30)
    private CourseType courseType;

    @Column(length = 255)
    private String description;

    private RuleType(String typeName, CourseType courseType, String description) {
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("typeName must not be null or blank");
        }
        this.typeName = typeName;
        this.courseType = courseType;
        this.description = description;
    }

    public static RuleType create(String typeName, CourseType courseType, String description) {
        return new RuleType(typeName, courseType, description);
    }
}
