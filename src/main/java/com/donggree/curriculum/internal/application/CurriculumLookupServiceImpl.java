package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.internal.domain.AreaType;
import com.donggree.curriculum.internal.domain.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.Course;
import com.donggree.curriculum.internal.domain.CourseRepository;
import com.donggree.curriculum.internal.domain.Department;
import com.donggree.curriculum.internal.domain.DepartmentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CurriculumLookupService 구현체.
 * 조회 시 해당 엔티티가 없으면 자동 생성하여 ID를 반환한다.
 */
@Service
@RequiredArgsConstructor
public class CurriculumLookupServiceImpl implements CurriculumLookupService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final AreaTypeRepository areaTypeRepository;

    @Override
    public Optional<Long> findDepartmentIdByName(String departmentName) {
        return departmentRepository.findByDepartmentName(departmentName)
                .map(Department::getId);
    }

    @Override
    public Long findOrCreateDepartmentId(String collegeName, String departmentName) {
        return departmentRepository.findByDepartmentName(departmentName)
                .map(Department::getId)
                .orElseGet(() -> departmentRepository.save(
                        Department.create(collegeName, departmentName)).getId());
    }

    @Override
    public Long findOrCreateCourseId(String courseCode, String courseName, int credits) {
        return courseRepository.findByCourseCode(courseCode)
                .map(Course::getId)
                .orElseGet(() -> courseRepository.save(
                        Course.create(courseCode, courseName, credits)).getId());
    }

    @Override
    public Long findOrCreateAreaTypeId(String areaName) {
        return areaTypeRepository.findByAreaName(areaName)
                .map(AreaType::getId)
                .orElseGet(() -> areaTypeRepository.save(
                        AreaType.create(areaName)).getId());
    }
}
