package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.internal.application.dto.CollegeResponse;
import com.donggree.curriculum.internal.application.dto.DepartmentResponse;
import com.donggree.curriculum.internal.domain.College;
import com.donggree.curriculum.internal.domain.CollegeRepository;
import com.donggree.curriculum.internal.domain.Department;
import com.donggree.curriculum.internal.domain.DepartmentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 단과대·학과 조회를 담당하는 응용 서비스.
 * 졸업 요건 세트 필터·등록 드롭다운의 선택지를 제공한다.
 */
@Service
@RequiredArgsConstructor
public class DepartmentAdminService {

    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;

    /** 단과대 목록(id·이름)을 이름순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<CollegeResponse> getColleges() {
        return collegeRepository.findAllByOrderByCollegeNameAsc().stream()
                .map(c -> new CollegeResponse(c.getId(), c.getCollegeName()))
                .toList();
    }

    /**
     * 학과 목록을 조회한다. collegeId가 주어지면 해당 단과대 소속 학과만, 없으면 전체를 학과명순으로 반환한다.
     * 각 항목은 단과대명까지 채워 내려준다.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments(Long collegeId) {
        List<Department> departments = (collegeId == null)
                ? departmentRepository.findAllByOrderByDepartmentNameAsc()
                : departmentRepository.findByCollegeIdOrderByDepartmentNameAsc(collegeId);

        Map<Long, String> collegeNames = loadCollegeNames(departments);
        return departments.stream()
                .map(d -> new DepartmentResponse(
                        d.getId(), d.getCollegeId(), collegeNames.get(d.getCollegeId()), d.getDepartmentName()))
                .toList();
    }

    private Map<Long, String> loadCollegeNames(List<Department> departments) {
        List<Long> collegeIds =
                departments.stream().map(Department::getCollegeId).distinct().toList();
        if (collegeIds.isEmpty()) {
            return Map.of();
        }
        return collegeRepository.findAllById(collegeIds).stream()
                .collect(Collectors.toMap(College::getId, College::getCollegeName));
    }
}
