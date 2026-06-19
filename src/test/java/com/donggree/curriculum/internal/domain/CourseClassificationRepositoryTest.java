package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.global.config.QueryDslConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QueryDslConfig.class)
class CourseClassificationRepositoryTest {

    @Autowired
    private CourseClassificationRepository repository;

    @BeforeEach
    void setUp() {
        // CS001: 2023~2025, FIRST_MAJOR, areaType 10
        repository.save(
                CourseClassification.create("CS001", 2023, 2025, CourseType.FIRST_MAJOR, 10L, null, null, "자료구조"));
        // CS002: 2024~2024, LIBERAL_ARTS, areaType 20
        repository.save(
                CourseClassification.create("CS002", 2024, 2024, CourseType.LIBERAL_ARTS, 20L, null, null, "글쓰기"));
        // CS003: 2026~2027, FIRST_MAJOR, areaType null
        repository.save(
                CourseClassification.create("CS003", 2026, 2027, CourseType.FIRST_MAJOR, null, null, null, "알고리즘"));
    }

    @Test
    void 필터가_없으면_전체를_조회한다() {
        assertThat(repository.search(null, null, null)).hasSize(3);
    }

    @Test
    void areaTypeId로_필터링한다() {
        List<CourseClassification> result = repository.search(List.of(10L), null, null);

        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001");
    }

    @Test
    void areaTypeId를_여러_개_지정하면_OR로_조회한다() {
        List<CourseClassification> result = repository.search(List.of(10L, 20L), null, null);

        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001", "CS002");
    }

    @Test
    void courseType으로_필터링한다() {
        List<CourseClassification> result = repository.search(null, List.of(CourseType.FIRST_MAJOR), null);

        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001", "CS003");
    }

    @Test
    void courseType을_여러_개_지정하면_OR로_조회한다() {
        List<CourseClassification> result =
                repository.search(null, List.of(CourseType.FIRST_MAJOR, CourseType.LIBERAL_ARTS), null);

        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001", "CS002", "CS003");
    }

    @Test
    void 단일_연도가_적용_범위에_포함되는_행만_조회한다() {
        List<CourseClassification> result = repository.search(null, null, List.of(2024));

        // CS001(2023~2025), CS002(2024~2024) 포함, CS003(2026~2027) 제외
        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001", "CS002");
    }

    @Test
    void 여러_연도를_지정하면_하나라도_범위에_들면_조회한다() {
        // 2024 -> CS001,CS002 / 2026 -> CS003
        List<CourseClassification> result = repository.search(null, null, List.of(2024, 2026));

        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001", "CS002", "CS003");
    }

    @Test
    void 연도_경계값도_포함한다() {
        assertThat(repository.search(null, null, List.of(2023)))
                .extracting(CourseClassification::getCourseCode)
                .containsExactly("CS001");
        assertThat(repository.search(null, null, List.of(2025)))
                .extracting(CourseClassification::getCourseCode)
                .containsExactly("CS001");
    }

    @Test
    void 여러_필터를_함께_적용한다() {
        List<CourseClassification> result =
                repository.search(List.of(10L), List.of(CourseType.FIRST_MAJOR), List.of(2023));

        assertThat(result).extracting(CourseClassification::getCourseCode).containsExactly("CS001");
    }

    @Test
    void 과목코드와_적용범위로_단건_조회한다() {
        var found = repository.findByCourseCodeAndStudentYearStartAndStudentYearEnd("CS001", 2023, 2025);

        assertThat(found).isPresent();
        assertThat(found.get().getTag()).isEqualTo("자료구조");
    }
}
