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
 * 이수 영역 분류를 나타내는 엔티티.
 * 교과목이 속하는 세부 영역(예: 자아, 영어, 리더, 대학, 동국 등)을 관리한다.
 */
@Entity
@Table(name = "area_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_name", nullable = false, length = 30)
    private String areaName;

    private AreaType(String areaName) {
        this.areaName = areaName;
    }

    public static AreaType create(String areaName) {
        return new AreaType(areaName);
    }
}
