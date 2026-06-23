package com.donggree.curriculum.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학과 정보를 나타내는 엔티티.
 * 소속 단과대(College)는 다른 애그리거트이므로 college_id로만 참조하고, 학과명을 관리한다.
 */
@Entity
@Table(name = "department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_id", nullable = false)
    private Long collegeId;

    @Column(name = "department_name", nullable = false, unique = true, length = 100)
    private String departmentName;

    private Department(Long collegeId, String departmentName) {
        this.collegeId = collegeId;
        this.departmentName = departmentName;
    }

    public static Department create(Long collegeId, String departmentName) {
        return new Department(collegeId, departmentName);
    }

    /**
     * 소속 단과대를 변경한다.
     * 학과명은 자연키라 바꾸지 않고, 기존 학과를 재사용할 때 소속 단과대만 최신값으로 맞추는 용도다.
     */
    public void updateCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }
}
