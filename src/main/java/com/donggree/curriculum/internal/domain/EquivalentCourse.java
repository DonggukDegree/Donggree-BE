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
 * 동치 과목 그룹 엔티티.
 * 연도별로 학수번호·과목명이 달라도 같은 내용으로 인정되는 과목들을 묶는다.
 * graduation_rule의 rule_config에서 equivalent_course_id로 참조하여
 * 특정 과목 이수 요건을 연도 변경 없이 관리한다.
 */
@Entity
@Table(name = "equivalent_course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquivalentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    private EquivalentCourse(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        this.name = name;
    }

    public static EquivalentCourse create(String name) {
        return new EquivalentCourse(name);
    }
}
