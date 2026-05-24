package com.donggree.curriculum.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DepartmentTest {

    @Test
    void 학과_생성_시_단과대학명과_학과명이_저장된다() {
        Department department = Department.create("소프트웨어융합대학", "컴퓨터·AI학부");

        assertThat(department.getCollegeName()).isEqualTo("소프트웨어융합대학");
        assertThat(department.getDepartmentName()).isEqualTo("컴퓨터·AI학부");
    }
}
