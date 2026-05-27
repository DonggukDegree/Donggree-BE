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
 * 교과목 카탈로그 엔티티. curriculum 모듈이 소유하며, admin이 직접 등록/관리한다.
 * transcript 모듈은 course_record에 course_name/credits를 반정규화하여 직접 저장하므로
 * 이 테이블을 참조하지 않는다. 향후 전공 과목 목록 조회 등 기능 확장 시 사용된다.
 */
@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true, length = 10)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 100)
    private String courseName;

    @Column(nullable = false)
    private int credits;

    private Course(String courseCode, String courseName, int credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException("credits must be positive");
        }
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
    }

    public static Course create(String courseCode, String courseName, int credits) {
        return new Course(courseCode, courseName, credits);
    }
}
