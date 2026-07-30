package com.donggree.curriculum.internal.application;

import com.donggree.curriculum.CourseType;
import com.donggree.curriculum.internal.application.projection.AreaTypeProjection;
import com.donggree.curriculum.internal.application.projection.CourseClassificationProjection;
import com.donggree.curriculum.internal.domain.areatype.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassificationRepositoryCustom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드의 과목 분류(course_classification)·이수 영역(area_type) 조회(Query) 응용 서비스.
 * 상태를 변경하지 않는 읽기 유스케이스만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CourseClassificationQueryService {

    private final CourseClassificationRepositoryCustom courseClassificationRepository;
    private final AreaTypeRepository areaTypeRepository;

    /**
     * 동적 다중 필터로 과목 분류를 조회한다. area_type을 조인해 영역 이름을 함께 담은 프로젝션으로
     * 엔티티 로딩 없이 바로 조회한다. 각 필터 목록이 비어 있으면(미지정) 전체를 반환한다.
     */
    @Transactional(readOnly = true)
    public List<CourseClassificationProjection> search(
            List<Long> areaTypeIds, List<CourseType> courseTypes, List<Integer> years) {
        return courseClassificationRepository.search(areaTypeIds, courseTypes, years);
    }

    /** 이수 영역 전체를 조회한다. 과목 분류 필터의 선택지로 사용된다. */
    @Transactional(readOnly = true)
    public List<AreaTypeProjection> getAreaTypes() {
        return areaTypeRepository.findAll().stream()
                .map(area -> new AreaTypeProjection(area.getId(), area.getAreaName()))
                .toList();
    }
}
