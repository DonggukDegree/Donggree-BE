package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.internal.domain.Department;
import com.donggree.curriculum.internal.domain.DepartmentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurriculumLookupServiceImpl implements CurriculumLookupService {

    private final DepartmentRepository departmentRepository;

    @Override
    public Optional<Long> findDepartmentIdByName(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return Optional.empty();
        }
        return departmentRepository.findByDepartmentName(departmentName)
                .map(Department::getId);
    }

    @Override
    public Map<Long, String> findDepartmentNamesByIds(List<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return new HashMap<>();
        }
        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));
    }
}
