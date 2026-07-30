package com.donggree.curriculum.internal.domain.requirementset;

import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 졸업 요건 세트를 나타내는 루트 애그리거트.
 * 특정 학과의 입학년도 범위에 적용되는 졸업 규칙들의 모음이다.
 * GraduationRule을 하위 엔티티로 소유하며, 반드시 이 엔티티를 통해 규칙을 추가한다.
 */
@Entity
@Table(
        name = "requirement_set",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_requirement_set_dept_year_version",
                        columnNames = {"department_id", "year_start", "year_end", "version"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequirementSet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "year_start", nullable = false)
    private int yearStart;

    @Column(name = "year_end", nullable = false)
    private int yearEnd;

    @Column(nullable = false)
    private int version;

    @Column(length = 255)
    private String description;

    @Column(name = "sheet_image_url", length = 512)
    private String sheetImageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ManyToMany
    @JoinTable(
            name = "requirement_set_rule",
            joinColumns = @JoinColumn(name = "requirement_set_id"),
            inverseJoinColumns = @JoinColumn(name = "graduation_rule_id"),
            uniqueConstraints =
                    @UniqueConstraint(
                            name = "uk_requirement_set_rule",
                            columnNames = {"requirement_set_id", "graduation_rule_id"}))
    private List<GraduationRule> rules = new ArrayList<>();

    private RequirementSet(
            Long departmentId,
            int yearStart,
            int yearEnd,
            int version,
            String description,
            String sheetImageUrl,
            boolean active) {
        assignValidated(departmentId, yearStart, yearEnd, version, description, sheetImageUrl, active);
    }

    public static RequirementSet create(
            Long departmentId,
            int yearStart,
            int yearEnd,
            int version,
            String description,
            String sheetImageUrl,
            boolean active) {
        return new RequirementSet(departmentId, yearStart, yearEnd, version, description, sheetImageUrl, active);
    }

    /**
     * 요건 세트의 스칼라 정보를 전체 교체한다(PUT). 생성과 동일한 불변식을 재검증한다.
     * 적용년도·학과·버전 등 식별 정보까지 수정할 수 있다. 연결 규칙은 {@link #replaceRules(List)}로 별도 교체한다.
     */
    public void update(
            Long departmentId,
            int yearStart,
            int yearEnd,
            int version,
            String description,
            String sheetImageUrl,
            boolean active) {
        assignValidated(departmentId, yearStart, yearEnd, version, description, sheetImageUrl, active);
    }

    /** 연결된 졸업 규칙 목록을 통째로 교체한다. 규칙 선택/해제(체크 토글)를 반영하는 데 사용한다. */
    public void replaceRules(List<GraduationRule> newRules) {
        this.rules.clear();
        this.rules.addAll(newRules);
    }

    private void assignValidated(
            Long departmentId,
            int yearStart,
            int yearEnd,
            int version,
            String description,
            String sheetImageUrl,
            boolean active) {
        if (departmentId == null) {
            throw new IllegalArgumentException("departmentId must not be null");
        }
        if (yearStart > yearEnd) {
            throw new IllegalArgumentException("yearStart must be less than or equal to yearEnd");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }
        this.departmentId = departmentId;
        this.yearStart = yearStart;
        this.yearEnd = yearEnd;
        this.version = version;
        this.description = description;
        this.sheetImageUrl = sheetImageUrl;
        this.active = active;
    }

    /**
     * 주어진 입학년도가 이 졸업 요건 세트의 적용 범위에 포함되는지 판별한다.
     */
    public boolean appliesTo(int admissionYear) {
        return admissionYear >= yearStart && admissionYear <= yearEnd;
    }

    /**
     * 이미 저장된 졸업 규칙을 이 요건 세트에 연결한다.
     * 동일한 규칙을 여러 RequirementSet에서 공유할 수 있다.
     */
    public void addRule(GraduationRule rule) {
        rules.add(rule);
    }

    /**
     * 졸업 규칙 목록의 불변 뷰를 반환한다.
     */
    public List<GraduationRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}
