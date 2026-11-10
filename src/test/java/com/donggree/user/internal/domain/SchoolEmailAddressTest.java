package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SchoolEmailAddressTest {
    @org.junit.jupiter.api.Test
    void 고정된_학교_도메인과_아이디로_주소를_만든다() {
        assertThat(SchoolEmailAddress.fromLocalPart("Student.123").value()).isEqualTo("student.123@dgu.ac.kr");
        assertThat(SchoolEmailAddress.fromLocalPart("a".repeat(30)).value()).hasSize(40);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "dongguk.edu",
                "dgu.edu",
                "gmail.com",
                "dgu.ac.kr.evil.invalid",
                "dgu.ac.kr\r\nBcc:attacker@example.invalid",
                "@dgu.ac.kr"
            })
    void 임의_도메인과_헤더_삽입을_거부한다(String domain) {
        assertThatThrownBy(() -> new SchoolEmailAddress("student@" + domain))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "student@gmail.com",
                " student",
                "student ",
                ".student",
                "student..id",
                "student+alias",
                "\r\nBcc:test"
            })
    void 아이디에_전체주소나_잘못된_값을_입력할_수_없다(String localPart) {
        assertThatThrownBy(() -> SchoolEmailAddress.fromLocalPart(localPart))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
