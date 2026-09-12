package com.donggree.transcript.internal.domain;

import com.donggree.global.entity.BaseEntity;
import com.donggree.transcript.internal.domain.enums.Grade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
@Table(name = "transcript", indexes = @Index(name = "idx_transcript_member_id", columnList = "member_id"))
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

    // PDF "과정" 원문. "학석사연계과정"(7자) 같은 값이 들어오므로 5자로는 부족하다.
    @Column(name = "student_type", length = 10)
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

    @Column(name = "english_pass_result")
    private Boolean englishPassResult;

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
        this.englishPassResult = data.englishPassResult();
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
     * courseTypeName은 PDF 원시 문자열 그대로 저장한다 (ex. "공교", "전필", "학기").
     */
    public CourseRecord addCourseRecord(
            String semester,
            String courseTypeName,
            String areaName,
            String courseCode,
            String courseName,
            int credits,
            Grade grade,
            boolean retake) {
        CourseRecord record =
                CourseRecord.create(semester, courseTypeName, areaName, courseCode, courseName, credits, grade, retake);
        courseRecords.add(record);
        record.assignTranscript(this);
        return record;
    }

    /**
     * 보유한 수강 이력을 전송된 목록으로 통째 치환한다.
     * 기존 이력은 모두 제거(orphanRemoval로 삭제)되고 전달된 목록만 남으므로,
     * 사용자가 수정·삭제·추가를 한 번에 반영할 수 있다.
     * 치환 후 학점 수와 평점 평균을 재계산하여 메타 값을 갱신한다.
     */
    public void replaceCourseRecords(List<CourseRecordData> newRecords) {
        courseRecords.clear();
        for (CourseRecordData data : newRecords) {
            addCourseRecord(
                    data.semester(),
                    data.courseTypeName(),
                    data.areaName(),
                    data.courseCode(),
                    data.courseName(),
                    data.credits(),
                    data.grade(),
                    data.retake());
        }
        recalculateCreditsAndGpa();
    }

    /**
     * 보유한 수강 이력을 기준으로 총취득학점과 평점 평균(GPA)을 다시 계산해 갱신한다.
     * 대학 학사 규칙에 따라 취득학점과 평점 계산용 학점(GPA 분모)을 분리한다.
     * - totalCredits: 이수에 성공한 학점의 합. 이수 실패인 F·NP 과목 학점은 제외한다.
     * - gpa: Σ(등급 평점 × 과목 학점) ÷ 평점 계산용 학점. 소수 셋째 자리에서 반올림하여 소수 둘째 자리까지.
     *   Pass/Fail 과목인 P·NP는 평점 계산(분모·분자)에서 완전히 제외한다.
     *   F는 취득학점에는 포함되지 않지만 평점 분모에는 포함되어 평점을 끌어내린다(평점 0).
     *   평점 계산용 학점이 0이면 GPA는 0.00이다.
     */
    private void recalculateCreditsAndGpa() {
        // 취득학점: F·NP(이수 실패)를 제외한 학점의 합
        this.totalCredits = courseRecords.stream()
                .filter(r -> r.getGrade() != Grade.F && r.getGrade() != Grade.NP)
                .mapToInt(CourseRecord::getCredits)
                .sum();

        // 평점 계산용 학점(GPA 분모): P·NP를 제외한 학점의 합 (F는 포함)
        int gpaDenominator = courseRecords.stream()
                .filter(r -> r.getGrade() != Grade.P && r.getGrade() != Grade.NP)
                .mapToInt(CourseRecord::getCredits)
                .sum();

        if (gpaDenominator == 0) {
            this.gpa = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        BigDecimal weightedSum = courseRecords.stream()
                .filter(r -> r.getGrade() != Grade.P && r.getGrade() != Grade.NP)
                .map(r -> r.getGrade().getGradePoint().multiply(BigDecimal.valueOf(r.getCredits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.gpa = weightedSum.divide(BigDecimal.valueOf(gpaDenominator), 2, RoundingMode.HALF_UP);
    }

    /**
     * 수강 이력 목록의 불변 뷰를 반환한다.
     */
    public List<CourseRecord> getCourseRecords() {
        return Collections.unmodifiableList(courseRecords);
    }

    /**
     * 보유한 수강 이력(CourseRecord)들의 학점 합을 반환한다.
     */
    public int recordedCredits() {
        return courseRecords.stream().mapToInt(CourseRecord::getCredits).sum();
    }

    /**
     * 총취득학점과 수강 이력 학점 합의 차이를 반환한다.
     * 양수면 PDF에 기재된 총취득학점보다 파싱된 과목 학점이 적어 이수 이력을 더 추가해야 함을,
     * 음수면 재수강·P/F 등으로 과목 학점 합이 총취득학점보다 많음을 의미한다.
     * 0이면 PDF 파싱 결과와 총취득학점이 정합함을 뜻한다.
     */
    public int creditGap() {
        return totalCredits - recordedCredits();
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
