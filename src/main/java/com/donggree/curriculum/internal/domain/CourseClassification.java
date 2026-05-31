package com.donggree.curriculum.internal.domain;

import com.donggree.curriculum.CourseType;
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
 * 과목별 입학년도·학과 기준 졸업 판정 분류 엔티티.
 * course_code를 직접 참조하여 course 테이블 조인 없이 분류를 조회한다.
 * classification이 없는 과목은 PDF의 course_type_name으로 courseType을 추론한다.
 * departmentId = null이면 전 학과 공통, non-null이면 해당 학과 전용 (전용이 공통보다 우선).
 */
@Entity
@Table(name = "course_classification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "student_year_start", nullable = false)
    private int studentYearStart;

    @Column(name = "student_year_end", nullable = false)
    private int studentYearEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 30)
    private CourseType courseType;

    @Column(name = "area_type_id")
    private Long areaTypeId;

    @Column(name = "sub_category", length = 20)
    private String subCategory;

    @Column(name = "subject_domain", length = 20)
    private String subjectDomain;

    private CourseClassification(
            String courseCode,
            Long departmentId,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("courseCode must not be null or blank");
        }
        if (courseType == null) {
            throw new IllegalArgumentException("courseType must not be null");
        }
        if (studentYearStart <= 0 || studentYearEnd <= 0) {
            throw new IllegalArgumentException("student years must be positive");
        }
        if (studentYearStart > studentYearEnd) {
            throw new IllegalArgumentException("studentYearStart must be <= studentYearEnd");
        }
        this.courseCode = courseCode;
        this.departmentId = departmentId;
        this.studentYearStart = studentYearStart;
        this.studentYearEnd = studentYearEnd;
        this.courseType = courseType;
        this.areaTypeId = areaTypeId;
        this.subCategory = subCategory;
        this.subjectDomain = subjectDomain;
    }

    public static CourseClassification create(
            String courseCode,
            Long departmentId,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain) {
        return new CourseClassification(
                courseCode,
                departmentId,
                studentYearStart,
                studentYearEnd,
                courseType,
                areaTypeId,
                subCategory,
                subjectDomain);
    }
}
