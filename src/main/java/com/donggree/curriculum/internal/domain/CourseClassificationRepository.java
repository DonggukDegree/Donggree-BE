package com.donggree.curriculum.internal.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseClassificationRepository extends JpaRepository<CourseClassification, Long> {

    List<CourseClassification> findByCourseIdIn(Collection<Long> courseIds);
}
