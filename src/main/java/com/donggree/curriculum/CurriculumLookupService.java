package com.donggree.curriculum;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 다른 모듈에서 curriculum 데이터를 조회하기 위한 공개 인터페이스.
 * Department는 사전에 등록된 데이터만 사용하며, 없으면 조회 실패로 처리한다.
 */
public interface CurriculumLookupService {

    /** 학과명으로 Department ID를 조회한다. 등록되지 않은 학과면 빈 Optional 반환. */
    Optional<Long> findDepartmentIdByName(String departmentName);

    /** ID 목록으로 Department 이름을 일괄 조회한다. 결과는 id → 학과명 맵으로 반환한다. */
    Map<Long, String> findDepartmentNamesByIds(List<Long> departmentIds);
}
