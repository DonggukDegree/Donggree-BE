package com.donggree.curriculum.internal.domain.courseclassification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseClassificationRepository extends JpaRepository<CourseClassification, Long> {

    List<CourseClassification> findByCourseCodeIn(Collection<String> courseCodes);

    /** 과목코드 + 적용 입학년도 범위로 분류를 조회한다. 등록/수정 시 unique 제약 충돌 사전 검증에 사용한다. */
    Optional<CourseClassification> findByCourseCodeAndStudentYearStartAndStudentYearEnd(
            String courseCode, int studentYearStart, int studentYearEnd);
}
