package com.donggree.curriculum.internal.domain.courseclassification;

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
 * 과목별 입학년도 기준 졸업 판정 분류 엔티티.
 * course_code를 직접 참조하여 과목의 courseType, areaName 등 분류 메타데이터를 제공한다.
 * classification이 없는 과목은 PDF의 course_type_name으로 courseType을 추론한다.
 */
@Entity
@Table(
        name = "course_classification",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_course_classification_code_years",
                        columnNames = {"course_code", "student_year_start", "student_year_end"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @Column(name = "tag", length = 100)
    private String tag;

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
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain,
            String tag) {
        assignValidated(
                courseCode, studentYearStart, studentYearEnd, courseType, areaTypeId, subCategory, subjectDomain, tag);
    }

    public static CourseClassification create(
            String courseCode,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain,
            String tag) {
        return new CourseClassification(
                courseCode, studentYearStart, studentYearEnd, courseType, areaTypeId, subCategory, subjectDomain, tag);
    }

    /**
     * 분류 정보를 전체 교체한다(PUT). 생성과 동일한 불변식을 재검증한다.
     * tag는 표시용 자유 텍스트로 검증하지 않고 그대로 보관한다.
     */
    public void update(
            String courseCode,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain,
            String tag) {
        assignValidated(
                courseCode, studentYearStart, studentYearEnd, courseType, areaTypeId, subCategory, subjectDomain, tag);
    }

    private void assignValidated(
            String courseCode,
            int studentYearStart,
            int studentYearEnd,
            CourseType courseType,
            Long areaTypeId,
            String subCategory,
            String subjectDomain,
            String tag) {
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
        this.studentYearStart = studentYearStart;
        this.studentYearEnd = studentYearEnd;
        this.courseType = courseType;
        this.areaTypeId = areaTypeId;
        this.subCategory = subCategory;
        this.subjectDomain = subjectDomain;
        this.tag = tag;
    }
}
