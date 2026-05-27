package com.donggree.transcript.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.transcript.internal.domain.enums.CourseType;
import com.donggree.transcript.internal.domain.enums.Grade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * PDF 파싱 결과를 저장하는 루트 애그리거트.
 * 회원 1명당 하나의 Transcript만 존재하며(member_id unique),
 * 개별 수강 이력(CourseRecord)을 하위 엔티티로 소유한다.
 */
@Entity
@Table(name = "transcript")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transcript extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

@JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private String rawData;

    @Column(name = "admission_year", nullable = false)
    private int admissionYear;

    @Column(name = "student_type", length = 5)
    private String studentType;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "sub_major1_id")
    private Long subMajor1Id;

    @Column(name = "sub_major2_id")
    private Long subMajor2Id;

    @Column(name = "dual_major1_id")
    private Long dualMajor1Id;

    @Column(name = "dual_major2_id")
    private Long dualMajor2Id;

    @Column(name = "total_credits", nullable = false)
    private int totalCredits;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal gpa;

    @Column(name = "completed_semesters", nullable = false)
    private int completedSemesters;

    @Column(name = "english_level", length = 5)
    private String englishLevel;

    @Column(name = "academic_status", nullable = false, length = 5)
    private String academicStatus;

    @Column(name = "engineering_certified", nullable = false)
    private boolean engineeringCertified;

    @Column(name = "is_transfer", nullable = false)
    private boolean transfer;

    @Column(name = "selective_completion", nullable = false)
    private boolean selectiveCompletion;

    @Column(name = "global_talent_track", nullable = false)
    private boolean globalTalentTrack;

    @Column(name = "english_course_target", nullable = false)
    private boolean englishCourseTarget;

    @Column(name = "completed_english_result")
    private Boolean completedEnglishResult;

    @Column(name = "teaching_aptitude_count")
    private Integer teachingAptitudeCount;

    @Column(name = "thesis_status", nullable = false)
    private boolean thesisStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "transcript", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseRecord> courseRecords = new ArrayList<>();

    private Transcript(TranscriptCreateData data) {
        if (data.memberId() == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        if (data.rawData() == null) {
            throw new IllegalArgumentException("rawData must not be null");
        }
        if (data.academicStatus() == null) {
            throw new IllegalArgumentException("academicStatus must not be null");
        }
        this.memberId = data.memberId();
        this.rawData = data.rawData();
        this.admissionYear = data.admissionYear();
        this.academicStatus = data.academicStatus();
        this.studentType = data.studentType();
        this.departmentId = data.departmentId();
        this.subMajor1Id = data.subMajor1Id();
        this.subMajor2Id = data.subMajor2Id();
        this.dualMajor1Id = data.dualMajor1Id();
        this.dualMajor2Id = data.dualMajor2Id();
        this.totalCredits = data.totalCredits();
        this.gpa = data.gpa();
        this.completedSemesters = data.completedSemesters();
        this.englishLevel = data.englishLevel();
        this.engineeringCertified = data.engineeringCertified();
        this.transfer = data.transfer();
        this.selectiveCompletion = data.selectiveCompletion();
        this.globalTalentTrack = data.globalTalentTrack();
        this.englishCourseTarget = data.englishCourseTarget();
        this.completedEnglishResult = data.completedEnglishResult();
        this.teachingAptitudeCount = data.teachingAptitudeCount();
        this.thesisStatus = data.thesisStatus();
    }

    /**
     * PDF 파싱 결과를 기반으로 Transcript를 생성하는 팩토리 메서드.
     * 파싱이 완료된 시점에 모든 데이터를 한 번에 받아 생성한다.
     */
    public static Transcript create(TranscriptCreateData data) {
        return new Transcript(data);
    }

    /**
     * 수강 이력을 생성하여 추가하고 양방향 관계를 설정한다.
     * CourseRecord는 Transcript의 하위 엔티티이므로 반드시 루트를 통해 생성한다.
     * 생성된 CourseRecord를 반환하여 저장 후 ID를 참조할 수 있도록 한다.
     */
    public CourseRecord addCourseRecord(String semester, CourseType courseType, String areaName,
                                        String courseCode, String courseName, int credits,
                                        Grade grade, boolean retake) {
        CourseRecord record = CourseRecord.create(semester, courseType, areaName,
                courseCode, courseName, credits, grade, retake);
        courseRecords.add(record);
        record.assignTranscript(this);
        return record;
    }

    /**
     * 수강 이력 목록의 불변 뷰를 반환한다.
     */
    public List<CourseRecord> getCourseRecords() {
        return Collections.unmodifiableList(courseRecords);
    }

    /**
     * 소프트 삭제를 수행한다. 삭제 시각을 외부에서 주입받아 기록한다.
     * 이미 삭제된 상태이면 IllegalStateException을 던진다.
     */
    public void markAsDeleted(LocalDateTime deletedAt) {
        if (isDeleted()) {
            throw new IllegalStateException("이미 삭제된 성적표입니다.");
        }
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
