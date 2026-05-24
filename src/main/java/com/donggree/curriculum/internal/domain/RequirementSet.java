package com.donggree.curriculum.internal.domain;

import com.donggree.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "requirement_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequirementSet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id")
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

    @OneToMany(mappedBy = "requirementSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GraduationRule> rules = new ArrayList<>();

    private RequirementSet(Long departmentId, int yearStart, int yearEnd,
                           int version, String description, String sheetImageUrl) {
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
        this.active = true;
    }

    public static RequirementSet create(Long departmentId, int yearStart, int yearEnd,
                                        int version, String description, String sheetImageUrl) {
        return new RequirementSet(departmentId, yearStart, yearEnd, version, description, sheetImageUrl);
    }

    /**
     * 주어진 입학년도가 이 졸업 요건 세트의 적용 범위에 포함되는지 판별한다.
     */
    public boolean appliesTo(int admissionYear) {
        return admissionYear >= yearStart && admissionYear <= yearEnd;
    }

    /**
     * 졸업 규칙을 추가하고 양방향 관계를 설정한다.
     */
    public void addRule(GraduationRule rule) {
        rules.add(rule);
        rule.assignRequirementSet(this);
    }

    /**
     * 졸업 규칙 목록의 불변 뷰를 반환한다.
     */
    public List<GraduationRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}
