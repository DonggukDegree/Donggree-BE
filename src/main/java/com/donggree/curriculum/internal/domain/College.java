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
 * 단과대학을 나타내는 엔티티. 학과(Department)의 상위 분류이며 학과는 college_id로 단과대를 참조한다.
 */
@Entity
@Table(name = "college")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_name", nullable = false, unique = true, length = 100)
    private String collegeName;

    private College(String collegeName) {
        this.collegeName = collegeName;
    }

    public static College create(String collegeName) {
        return new College(collegeName);
    }
}
