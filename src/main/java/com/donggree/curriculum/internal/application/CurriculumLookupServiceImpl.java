package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseView;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.curriculum.internal.domain.Course;
import com.donggree.curriculum.internal.domain.CourseClassification;
import com.donggree.curriculum.internal.domain.CourseClassificationRepository;
import com.donggree.curriculum.internal.domain.CourseRepository;
import com.donggree.curriculum.internal.domain.Department;
import com.donggree.curriculum.internal.domain.DepartmentRepository;
import com.donggree.curriculum.internal.domain.GraduationRule;
import com.donggree.curriculum.internal.domain.RequirementSetRepository;
import com.donggree.curriculum.internal.domain.RuleType;
import com.donggree.curriculum.internal.domain.RuleTypeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumLookupServiceImpl implements CurriculumLookupService {

    private final DepartmentRepository departmentRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final RuleTypeRepository ruleTypeRepository;
    private final CourseRepository courseRepository;
    private final CourseClassificationRepository courseClassificationRepository;

    @Override
    public Optional<Long> findDepartmentIdByName(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return Optional.empty();
        }
        return departmentRepository.findByDepartmentName(departmentName).map(Department::getId);
    }

    @Override
    public Map<Long, String> findDepartmentNamesByIds(List<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return new HashMap<>();
        }
        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));
    }

    @Override
    public Optional<RequirementSetView> findActiveRequirementSet(Long departmentId, int admissionYear) {
        return requirementSetRepository.findByDepartmentIdAndActiveTrue(departmentId).stream()
                .filter(rs -> rs.appliesTo(admissionYear))
                .findFirst()
                .map(rs ->
                        new RequirementSetView(rs.getId(), rs.getDepartmentId(), rs.getYearStart(), rs.getYearEnd()));
    }

    @Override
    public List<GraduationRuleView> findGraduationRules(Long requirementSetId) {
        return requirementSetRepository
                .findById(requirementSetId)
                .map(rs -> {
                    List<GraduationRule> rules = rs.getRules();
                    if (rules.isEmpty()) {
                        return List.<GraduationRuleView>of();
                    }
                    List<Long> ruleTypeIds = rules.stream()
                            .map(GraduationRule::getRuleTypeId)
                            .distinct()
                            .toList();
                    Map<Long, RuleType> ruleTypeById = ruleTypeRepository.findAllById(ruleTypeIds).stream()
                            .collect(Collectors.toMap(RuleType::getId, Function.identity()));
                    return rules.stream()
                            .map(rule -> {
                                RuleType rt = ruleTypeById.get(rule.getRuleTypeId());
                                return new GraduationRuleView(
                                        rule.getId(),
                                        rt != null ? rt.getTypeName() : "UNKNOWN",
                                        rt != null ? rt.getCourseType() : null,
                                        rule.getRuleName(),
                                        rule.getRuleConfig());
                            })
                            .toList();
                })
                .orElse(List.of());
    }

    @Override
    public Map<String, CourseView> findCoursesByCodes(List<String> courseCodes) {
        if (courseCodes == null || courseCodes.isEmpty()) {
            return Map.of();
        }
        return courseRepository.findByCourseCodeIn(courseCodes).stream()
                .collect(Collectors.toMap(
                        Course::getCourseCode,
                        c -> new CourseView(
                                c.getId(),
                                c.getCourseCode(),
                                c.getCourseName(),
                                c.getCredits(),
                                c.getEquivalentCourseId())));
    }

    @Override
    public Map<Long, CourseClassificationView> findCourseClassificationsByCourseIds(
            List<Long> courseIds, int admissionYear) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        return courseClassificationRepository.findByCourseIdIn(courseIds).stream()
                .filter(cc -> admissionYear >= cc.getStudentYearStart() && admissionYear <= cc.getStudentYearEnd())
                .collect(Collectors.toMap(
                        CourseClassification::getCourseId,
                        cc -> new CourseClassificationView(
                                cc.getCourseId(),
                                cc.getCourseType(),
                                cc.getAreaTypeId(),
                                cc.getSubCategory(),
                                cc.getSubjectDomain()),
                        (existing, replacement) -> existing));
    }
}
