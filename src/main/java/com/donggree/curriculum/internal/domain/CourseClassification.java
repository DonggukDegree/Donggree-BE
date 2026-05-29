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
 * 과목별 입학년도 기준 졸업 판정 분류 엔티티.
 * PDF의 course_type_name·area_name은 수강년도 기준이라 졸업 판정에 쓸 수 없다.
 * graduation 모듈은 course_record.course_code → course.id → 이 테이블 순으로
 * 해당 학생 입학년도에 맞는 분류를 조회하여 판정한다.
 * sub_category, subject_domain은 과학 영역의 실험/개론 구분 및 충돌 규칙 평가에 사용한다.
 */
@Entity
@Table(name = "course_classification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

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
            Long courseId,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId must not be null");
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
        this.courseId = courseId;
        this.studentYearStart = studentYearStart;
        this.studentYearEnd = studentYearEnd;
        this.courseType = courseType;
        this.areaTypeId = areaTypeId;
        this.subCategory = subCategory;
        this.subjectDomain = subjectDomain;
    }

    public static CourseClassification create(
            Long courseId,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain) {
        return new CourseClassification(
                courseId, studentYearStart, studentYearEnd, courseType, areaTypeId, subCategory, subjectDomain);
    }
}
