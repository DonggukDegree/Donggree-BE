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
 * 한 학기에 이수한 개별 교과목의 성적 정보를 담는다.
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

    @Column(name = "area_type_id")
    private Long areaTypeId;

    @Column(name = "course_id")
    private Long courseId;

    @Convert(converter = GradeConverter.class)
    @Column(nullable = false, length = 2)
    private Grade grade;

    @Column(name = "is_retake", nullable = false)
    private boolean retake;

    private CourseRecord(String semester, CourseType courseType, Long areaTypeId,
                         Long courseId, Grade grade, boolean retake) {
        if (semester == null) {
            throw new IllegalArgumentException("semester must not be null");
        }
        if (courseType == null) {
            throw new IllegalArgumentException("courseType must not be null");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("courseId must not be null");
        }
        if (grade == null) {
            throw new IllegalArgumentException("grade must not be null");
        }
        this.semester = semester;
        this.courseType = courseType;
        this.areaTypeId = areaTypeId;
        this.courseId = courseId;
        this.grade = grade;
        this.retake = retake;
    }

    static CourseRecord create(String semester, CourseType courseType, Long areaTypeId,
                               Long courseId, Grade grade, boolean retake) {
        return new CourseRecord(semester, courseType, areaTypeId, courseId, grade, retake);
    }

    void assignTranscript(Transcript transcript) {
        this.transcript = transcript;
    }
}
