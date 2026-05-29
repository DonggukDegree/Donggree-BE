package com.donggree.curriculum.internal.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCourseCodeIn(Collection<String> courseCodes);
}
