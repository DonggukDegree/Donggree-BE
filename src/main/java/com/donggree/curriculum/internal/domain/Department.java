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
 * 소속 단과대학과 학과명을 관리한다.
 */
@Entity
@Table(name = "department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_name", nullable = false, length = 100)
    private String collegeName;

    @Column(name = "department_name", nullable = false, unique = true, length = 100)
    private String departmentName;

    private Department(String collegeName, String departmentName) {
        this.collegeName = collegeName;
        this.departmentName = departmentName;
    }

    public static Department create(String collegeName, String departmentName) {
        return new Department(collegeName, departmentName);
    }
}
