package com.donggree.transcript.internal.domain;

import com.donggree.transcript.internal.domain.enums.CourseType;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.domain.enums.GradeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개별 수강 이력을 나타내는 엔티티.
 * Transcript 애그리거트의 하위 엔티티로, 반드시 Transcript를 통해 생성된다.
 * course_name, credits, area_name은 업로드 시점 스냅샷으로 직접 저장한다(반정규화).
 * course_code는 graduation 모듈의 규칙 매칭용으로 보관한다.
 */
@Entity
@Table(name = "course_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcript_id")
    private Transcript transcript;

    @Column(nullable = false, length = 10)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type")
    private CourseType courseType;

    @Column(name = "area_name", length = 30)
    private String areaName;

    @Column(name = "course_code", length = 10)
    private String courseCode;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column
    private int credits;

    @Convert(converter = GradeConverter.class)
    @Column(nullable = false, length = 2)
    private Grade grade;

    @Column(name = "is_retake", nullable = false)
    private boolean retake;

    private CourseRecord(String semester, CourseType courseType, String areaName,
                         String courseCode, String courseName, int credits,
                         Grade grade, boolean retake) {
        if (semester == null) {
            throw new IllegalArgumentException("semester must not be null");
        }
        if (courseType == null) {
            throw new IllegalArgumentException("courseType must not be null");
        }
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("courseCode must not be null or blank");
        }
        if (grade == null) {
            throw new IllegalArgumentException("grade must not be null");
        }
        this.semester = semester;
        this.courseType = courseType;
        this.areaName = areaName;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.grade = grade;
        this.retake = retake;
    }

    static CourseRecord create(String semester, CourseType courseType, String areaName,
                               String courseCode, String courseName, int credits,
                               Grade grade, boolean retake) {
        return new CourseRecord(semester, courseType, areaName, courseCode, courseName, credits, grade, retake);
    }

    void assignTranscript(Transcript transcript) {
        this.transcript = transcript;
    }
}
