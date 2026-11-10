package com.donggree.curriculum;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 다른 모듈에서 curriculum 데이터를 조회하기 위한 공개 인터페이스.
 * graduation 모듈이 졸업 판정에 필요한 requirement_set, graduation_rule, course_classification을 조회할 때 사용한다.
 */
public interface CurriculumLookupService {

    /**
     * 학과명이 유일하면 기존 학과를 반환하고, 동명이면 교육과정 적용년도·과정의 활성 세트로 좁힌다.
     * 남은 후보는 PDF 단과대명으로 구분한다. 단과대명이 없는 복수전공은 null 전달.
     * 미등록 또는 끝까지 구분할 수 없는 학과는 빈 Optional 반환. 다른 년도 세트로 대체하지 않는다.
     */
    Optional<Long> findDepartmentId(
            String departmentName, int admissionYear, boolean engineeringCertified, String collegeName);

    /** ID 목록으로 Department 이름을 일괄 조회한다. 결과는 id → 학과명 맵으로 반환한다. */
    Map<Long, String> findDepartmentNamesByIds(List<Long> departmentIds);

    /** Department ID로 소속 단과대학명을 조회한다. 등록되지 않은 학과면 빈 Optional 반환. */
    Optional<String> findCollegeNameByDepartmentId(Long departmentId);

    /**
     * 학과·입학년도·과정에 적용되는 활성 졸업 요건 세트를 조회한다.
     * yearStart ≤ admissionYear ≤ yearEnd 조건을 만족하면서 학생의 과정에도 맞는 활성 세트를 반환한다.
     *
     * @param engineeringCertified 성적표의 공학인증심화대상 여부(true면 심화과정)
     */
    Optional<RequirementSetView> findActiveRequirementSet(
            Long departmentId, int admissionYear, boolean engineeringCertified);

    /** requirement_set_id에 속한 졸업 규칙 전체를 조회한다. */
    List<GraduationRuleView> findGraduationRules(Long requirementSetId);

    /**
     * course_code 목록과 입학년도로 과목 분류를 일괄 조회한다.
     * course_classification에 등록된 과목만 반환하며, 미등록 과목은 포함되지 않는다.
     * 결과는 courseCode → CourseClassificationView 맵으로 반환한다.
     */
    Map<String, CourseClassificationView> findCourseClassifications(List<String> courseCodes, int admissionYear);
}
