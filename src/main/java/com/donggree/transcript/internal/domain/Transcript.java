package com.donggree.transcript.internal.domain;

import com.donggree.global.entity.BaseEntity;
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

    @Column(name = "member_id", unique = true)
    private Long memberId;

    @Column(name = "pdf_url", nullable = false, length = 512)
    private String pdfUrl;

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

    @Column(name = "completed_english_major")
    private Integer completedEnglishMajor;

    @Column(name = "completed_english_non_major")
    private Integer completedEnglishNonMajor;

    @Column(name = "teaching_aptitude_count")
    private Integer teachingAptitudeCount;

    @Column(name = "thesis_status", nullable = false)
    private boolean thesisStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "transcript", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseRecord> courseRecords = new ArrayList<>();

    private Transcript(Long memberId, String pdfUrl, String rawData,
                       int admissionYear, String academicStatus) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        this.memberId = memberId;
        this.pdfUrl = pdfUrl;
        this.rawData = rawData;
        this.admissionYear = admissionYear;
        this.academicStatus = academicStatus;
        this.totalCredits = 0;
        this.gpa = BigDecimal.ZERO;
        this.completedSemesters = 0;
        this.engineeringCertified = false;
        this.transfer = false;
        this.selectiveCompletion = false;
        this.globalTalentTrack = false;
        this.englishCourseTarget = false;
        this.thesisStatus = false;
    }

    /**
     * 필수 필드만 받아 Transcript를 생성하는 팩토리 메서드.
     * PDF 파싱 직후 최소한의 정보로 생성하며, 나머지 필드는 파싱 결과에 따라 별도 메서드로 설정한다.
     */
    public static Transcript create(Long memberId, String pdfUrl, String rawData,
                                    int admissionYear, String academicStatus) {
        return new Transcript(memberId, pdfUrl, rawData, admissionYear, academicStatus);
    }

    /**
     * 수강 이력을 추가하고 양방향 관계를 설정한다.
     */
    public void addCourseRecord(CourseRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("courseRecord must not be null");
        }
        courseRecords.add(record);
        record.assignTranscript(this);
    }

    /**
     * 수강 이력 목록의 불변 뷰를 반환한다.
     */
    public List<CourseRecord> getCourseRecords() {
        return Collections.unmodifiableList(courseRecords);
    }

    /**
     * 소프트 삭제를 수행한다. deletedAt을 현재 시각으로 기록한다.
     * 이미 삭제된 상태이면 IllegalStateException을 던진다.
     */
    public void markAsDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("이미 삭제된 성적표입니다.");
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
