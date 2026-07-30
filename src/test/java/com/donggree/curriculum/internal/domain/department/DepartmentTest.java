package com.donggree.curriculum.internal.domain.department;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DepartmentTest {

    @Test
    void 학과_생성_시_소속_단과대ID와_학과명이_저장된다() {
        Department department = Department.create(1L, "컴퓨터·AI학부");

        assertThat(department.getCollegeId()).isEqualTo(1L);
        assertThat(department.getDepartmentName()).isEqualTo("컴퓨터·AI학부");
    }
}
