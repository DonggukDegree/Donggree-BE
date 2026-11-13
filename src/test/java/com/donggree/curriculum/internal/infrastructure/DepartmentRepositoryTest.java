package com.donggree.curriculum.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.curriculum.internal.domain.college.College;
import com.donggree.curriculum.internal.domain.college.CollegeRepository;
import com.donggree.curriculum.internal.domain.department.Department;
import com.donggree.curriculum.internal.domain.department.DepartmentRepository;
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

    @Autowired
    private CollegeRepository collegeRepository;

    // department.college_id는 college를 참조하므로(fk_department_college) 단과대를 먼저 만든다.
    private Long college(String name) {
        return collegeRepository.saveAndFlush(College.create(name)).getId();
    }

    @Test
    void 단과대가_다르면_같은_학과명을_등록한다() {
        Long first = college("공과대학");
        Long second = college("AI융합대학");
        Department original = repository.saveAndFlush(Department.create(first, "컴퓨터공학전공"));
        Department moved = repository.saveAndFlush(Department.create(second, "컴퓨터공학전공"));

        assertThat(moved.getId()).isNotEqualTo(original.getId());
        assertThat(repository.findById(original.getId()).orElseThrow().getCollegeId())
                .isEqualTo(first);
    }

    @Test
    void 같은_단과대의_같은_학과명은_중복_등록할_수_없다() {
        Long collegeId = college("공과대학");
        repository.saveAndFlush(Department.create(collegeId, "컴퓨터공학전공"));
        assertThatThrownBy(() -> repository.saveAndFlush(Department.create(collegeId, "컴퓨터공학전공")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
