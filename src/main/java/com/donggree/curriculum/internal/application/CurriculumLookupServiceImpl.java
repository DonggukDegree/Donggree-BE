package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GraduationRuleView;
import com.donggree.curriculum.RequirementSetView;
import com.donggree.curriculum.internal.domain.areatype.AreaType;
import com.donggree.curriculum.internal.domain.areatype.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.college.College;
import com.donggree.curriculum.internal.domain.college.CollegeRepository;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassification;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassificationRepository;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
import com.donggree.curriculum.internal.domain.graduationrule.GraduationRule;
import com.donggree.curriculum.internal.domain.requirementset.RequirementSetRepository;
import com.donggree.curriculum.internal.domain.ruletype.RuleType;
import com.donggree.curriculum.internal.domain.ruletype.RuleTypeRepository;
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
    private final CollegeRepository collegeRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final RuleTypeRepository ruleTypeRepository;
    private final CourseClassificationRepository courseClassificationRepository;
    private final AreaTypeRepository areaTypeRepository;

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
    public Optional<String> findCollegeNameByDepartmentId(Long departmentId) {
        if (departmentId == null) {
            return Optional.empty();
        }
        return departmentRepository
                .findById(departmentId)
                .map(Department::getCollegeId)
                .flatMap(collegeRepository::findById)
                .map(College::getCollegeName);
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
    public Map<String, CourseClassificationView> findCourseClassifications(
            List<String> courseCodes, int admissionYear) {
        if (courseCodes == null || courseCodes.isEmpty()) {
            return Map.of();
        }

        Map<String, CourseClassification> byCode =
                courseClassificationRepository.findByCourseCodeIn(courseCodes).stream()
                        .filter(cc ->
                                admissionYear >= cc.getStudentYearStart() && admissionYear <= cc.getStudentYearEnd())
                        .collect(Collectors.toMap(
                                CourseClassification::getCourseCode, Function.identity(), (a, b) -> a));

        // area_type_id → area_name 일괄 조회
        List<Long> areaTypeIds = byCode.values().stream()
                .map(CourseClassification::getAreaTypeId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> areaNameById = areaTypeRepository.findAllById(areaTypeIds).stream()
                .collect(Collectors.toMap(AreaType::getId, AreaType::getAreaName));

        return byCode.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            CourseClassification cc = e.getValue();
            return new CourseClassificationView(
                    cc.getCourseType(),
                    cc.getAreaTypeId() != null ? areaNameById.get(cc.getAreaTypeId()) : null,
                    cc.getSubCategory(),
                    cc.getSubjectDomain());
        }));
    }
}
