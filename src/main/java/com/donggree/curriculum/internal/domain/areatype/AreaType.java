package com.donggree.curriculum.internal.domain.areatype;

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
 * 이수 영역 카탈로그 엔티티. curriculum 모듈이 소유하며, admin이 직접 등록/관리한다.
 * transcript 모듈은 course_record에 area_name을 직접 저장하므로 이 테이블을 참조하지 않는다.
 * 향후 이수 영역 목록 조회 등 기능 확장 시 사용된다.
 */
@Entity
@Table(name = "area_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_name", nullable = false, unique = true, length = 30)
    private String areaName;

    private AreaType(String areaName) {
        this.areaName = areaName;
    }

    public static AreaType create(String areaName) {
        return new AreaType(areaName);
    }
}
