package com.donggree.curriculum.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.donggree.curriculum.internal.domain.areatype.AreaType;
import com.donggree.curriculum.internal.domain.areatype.AreaTypeRepository;
import com.donggree.curriculum.internal.domain.courseclassification.CourseClassificationRepositoryCustom;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * search()는 리포지토리가 QueryDSL 프로젝션으로 직접 조회하므로(엔티티 미로딩) 그 검증은
 * {@code CourseClassificationRepositoryTest}(@DataJpaTest)에서 다룬다. 여기서는 서비스 고유 로직만 검증한다.
 */
class CourseClassificationQueryServiceTest {

    private final CourseClassificationRepositoryCustom courseClassificationRepository =
            Mockito.mock(CourseClassificationRepositoryCustom.class);
    private final AreaTypeRepository areaTypeRepository = Mockito.mock(AreaTypeRepository.class);
    private final CourseClassificationQueryService service =
            new CourseClassificationQueryService(courseClassificationRepository, areaTypeRepository);

    private AreaType areaType(Long id, String name) {
        AreaType area = AreaType.create(name);
        ReflectionTestUtils.setField(area, "id", id);
        return area;
    }

    @Test
    void 이수영역_전체를_조회한다() {
        given(areaTypeRepository.findAll()).willReturn(List.of(areaType(10L, "전공기초"), areaType(20L, "기본소양")));

        var result = service.getAreaTypes();

        assertThat(result).extracting(r -> r.areaName()).containsExactly("전공기초", "기본소양");
    }
}
