package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.dto.AreaTypeResponse;
import com.donggree.curriculum.internal.application.dto.CourseClassificationCommand;
import com.donggree.curriculum.internal.application.dto.CourseClassificationResponse;
import com.donggree.curriculum.internal.application.dto.CourseClassificationUpsertCommand;
import com.donggree.curriculum.internal.application.exception.CurriculumErrorCode;
import com.donggree.curriculum.internal.domain.AreaType;
import com.donggree.curriculum.internal.domain.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.CourseClassification;
import com.donggree.curriculum.internal.domain.CourseClassificationRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 과목 분류(course_classification)·이수 영역(area_type) 관리를 담당하는 응용 서비스.
 * 트랜잭션 경계와 도메인 조립을 담당하고, 비즈니스 규칙은 도메인(CourseClassification)에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class CourseClassificationAdminService {

    private final CourseClassificationRepository courseClassificationRepository;
    private final AreaTypeRepository areaTypeRepository;

    /**
     * 동적 다중 필터로 과목 분류를 조회한다. area_type_id 대신 영역 이름을 함께 채워 반환한다.
     * 각 필터 목록이 비어 있으면(미지정) 전체를 반환한다.
     */
    @Transactional(readOnly = true)
    public List<CourseClassificationResponse> search(
            List<Long> areaTypeIds, List<CourseType> courseTypes, List<Integer> years) {
        List<CourseClassification> rows = courseClassificationRepository.search(areaTypeIds, courseTypes, years);
        Map<Long, String> areaNames = loadAreaNames(rows);
        return rows.stream()
                .map(cc -> toResponse(cc, cc.getAreaTypeId() == null ? null : areaNames.get(cc.getAreaTypeId())))
                .toList();
    }

    /**
     * 과목 분류를 배치로 업서트한다. 항목의 id가 null이면 신규 등록, non-null이면 전체 교체(수정)한다.
     * 전체가 하나의 트랜잭션으로 처리되어 일부라도 실패하면 모두 롤백된다.
     * 등록/수정을 구분하지 않고 한 번에 여러 건을 변경하는 대시보드 저장에 사용한다.
     *
     * @return 각 항목의 결과 ID 목록(입력 순서와 동일)
     */
    @Transactional
    public List<Long> upsert(List<CourseClassificationUpsertCommand> items) {
        validateNoDuplicateKeysInBatch(items);

        List<Long> ids = new ArrayList<>();
        for (CourseClassificationUpsertCommand item : items) {
            ids.add(item.id() == null ? create(item.data()) : update(item.id(), item.data()));
        }
        return ids;
    }

    private Long create(CourseClassificationCommand command) {
        validateAreaType(command.areaTypeId());
        courseClassificationRepository
                .findByCourseCodeAndStudentYearStartAndStudentYearEnd(
                        command.courseCode(), command.studentYearStart(), command.studentYearEnd())
                .ifPresent(existing -> {
                    throw new GeneralException(CurriculumErrorCode.DUPLICATE_COURSE_CLASSIFICATION);
                });

        CourseClassification saved = courseClassificationRepository.save(CourseClassification.create(
                command.courseCode(),
                command.studentYearStart(),
                command.studentYearEnd(),
                command.courseType(),
                command.areaTypeId(),
                command.subCategory(),
                command.subjectDomain(),
                command.tag()));
        return saved.getId();
    }

    private Long update(Long id, CourseClassificationCommand command) {
        CourseClassification classification = courseClassificationRepository
                .findById(id)
                .orElseThrow(() -> new GeneralException(CurriculumErrorCode.COURSE_CLASSIFICATION_NOT_FOUND));

        validateAreaType(command.areaTypeId());
        courseClassificationRepository
                .findByCourseCodeAndStudentYearStartAndStudentYearEnd(
                        command.courseCode(), command.studentYearStart(), command.studentYearEnd())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new GeneralException(CurriculumErrorCode.DUPLICATE_COURSE_CLASSIFICATION);
                });

        classification.update(
                command.courseCode(),
                command.studentYearStart(),
                command.studentYearEnd(),
                command.courseType(),
                command.areaTypeId(),
                command.subCategory(),
                command.subjectDomain(),
                command.tag());
        return id;
    }

    /** 한 배치 안에 (과목코드, 적용 시작년도, 적용 종료년도)가 중복된 항목이 있으면 예외를 던진다. */
    private void validateNoDuplicateKeysInBatch(List<CourseClassificationUpsertCommand> items) {
        Set<String> keys = new HashSet<>();
        for (CourseClassificationUpsertCommand item : items) {
            CourseClassificationCommand data = item.data();
            String key = data.courseCode() + "|" + data.studentYearStart() + "|" + data.studentYearEnd();
            if (!keys.add(key)) {
                throw new GeneralException(CurriculumErrorCode.DUPLICATE_COURSE_CLASSIFICATION);
            }
        }
    }

    /** 이수 영역 전체를 조회한다. 과목 분류 필터의 선택지로 사용된다. */
    @Transactional(readOnly = true)
    public List<AreaTypeResponse> getAreaTypes() {
        return areaTypeRepository.findAll().stream()
                .map(area -> new AreaTypeResponse(area.getId(), area.getAreaName()))
                .toList();
    }

    private void validateAreaType(Long areaTypeId) {
        if (areaTypeId != null && !areaTypeRepository.existsById(areaTypeId)) {
            throw new GeneralException(CurriculumErrorCode.AREA_TYPE_NOT_FOUND);
        }
    }

    private Map<Long, String> loadAreaNames(List<CourseClassification> rows) {
        List<Long> areaTypeIds = rows.stream()
                .map(CourseClassification::getAreaTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (areaTypeIds.isEmpty()) {
            return Map.of();
        }
        return areaTypeRepository.findAllById(areaTypeIds).stream()
                .collect(Collectors.toMap(AreaType::getId, AreaType::getAreaName));
    }

    private CourseClassificationResponse toResponse(CourseClassification cc, String areaName) {
        return new CourseClassificationResponse(
                cc.getId(),
                cc.getCourseCode(),
                cc.getTag(),
                cc.getStudentYearStart(),
                cc.getStudentYearEnd(),
                cc.getCourseType(),
                cc.getAreaTypeId(),
                areaName,
                cc.getSubCategory(),
                cc.getSubjectDomain());
    }
}
