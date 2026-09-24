package com.donggree.curriculum.internal.domain.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class DepartmentRepositoryTest {
    @Autowired
    private DepartmentRepository repository;

    @Test
    void 단과대가_다르면_같은_학과명을_등록한다() {
        Department original = repository.saveAndFlush(Department.create(1L, "컴퓨터공학전공"));
        Department moved = repository.saveAndFlush(Department.create(2L, "컴퓨터공학전공"));

        assertThat(moved.getId()).isNotEqualTo(original.getId());
        assertThat(repository.findById(original.getId()).orElseThrow().getCollegeId())
                .isEqualTo(1L);
    }

    @Test
    void 같은_단과대의_같은_학과명은_중복_등록할_수_없다() {
        repository.saveAndFlush(Department.create(1L, "컴퓨터공학전공"));
        assertThatThrownBy(() -> repository.saveAndFlush(Department.create(1L, "컴퓨터공학전공")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
