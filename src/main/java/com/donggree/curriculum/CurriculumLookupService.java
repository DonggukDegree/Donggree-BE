package com.donggree.curriculum;

import java.util.Optional;

/**
 * 다른 모듈에서 curriculum 데이터를 조회·생성하기 위한 공개 인터페이스.
 * 조회 시 해당 엔티티가 없으면 자동 생성하여 ID를 반환한다.
 */
public interface CurriculumLookupService {

    /** 학과명으로 Department ID를 조회한다. 없으면 빈 Optional 반환. */
    Optional<Long> findDepartmentIdByName(String departmentName);

    /** 대학명 + 학과명으로 Department ID를 조회한다. 없으면 새로 생성하고 ID를 반환한다. */
    Long findOrCreateDepartmentId(String collegeName, String departmentName);

    /** 과목 코드로 Course ID를 조회한다. 없으면 새로 생성하고 ID를 반환한다. */
    Long findOrCreateCourseId(String courseCode, String courseName, int credits);

    /** 영역명으로 AreaType ID를 조회한다. 없으면 새로 생성하고 ID를 반환한다. */
    Long findOrCreateAreaTypeId(String areaName);
}
