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
 * 교과목 정보를 나타내는 엔티티.
 * 교과목 코드, 교과목명, 학점을 관리한다.
 */
@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, length = 7)
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
