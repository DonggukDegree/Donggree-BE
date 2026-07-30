package com.donggree.curriculum.internal.domain.department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 학과 정보를 나타내는 엔티티.
 * 소속 단과대(College)는 다른 애그리거트이므로 college_id로만 참조하고, 학과명을 관리한다.
 */
@Entity
@Table(name = "department", indexes = @Index(name = "idx_department_college_id", columnList = "college_id"))
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
}
