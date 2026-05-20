package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void 카카오_회원_등록_시_기본_권한과_온보딩_미완료_상태로_초기화한다() {
        Member member = createMember();

        assertThat(member.getStudentId()).isNull();
        assertThat(member.getName()).isEqualTo("Alice");
        assertThat(member.getNickname()).isNull();
        assertThat(member.getEmail()).isEqualTo("alice@example.com");
        assertThat(member.getOauthId()).isEqualTo("kakao-123");
        assertThat(member.getProfileUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(member.getRole()).isEqualTo(Role.STUDENT);
        assertThat(member.hasCompletedOnboarding()).isFalse();
    }

    @Test
    void 카카오_회원_등록_시_문자열_앞뒤_공백을_제거한다() {
        Member member = Member.registerKakaoMember(
                " kakao-123 ",
                " alice@example.com ",
                " Alice ",
                " https://example.com/profile.png "
        );

        assertThat(member.getOauthId()).isEqualTo("kakao-123");
        assertThat(member.getEmail()).isEqualTo("alice@example.com");
        assertThat(member.getName()).isEqualTo("Alice");
        assertThat(member.getProfileUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    void 온보딩_완료_시_학번을_저장한다() {
        Member member = createMember();

        member.completeOnboarding("2023123456");

        assertThat(member.getStudentId()).isEqualTo("2023123456");
        assertThat(member.hasCompletedOnboarding()).isTrue();
    }

    @Test
    void 온보딩_완료_시_학번의_앞뒤_공백을_제거한다() {
        Member member = createMember();

        member.completeOnboarding(" 2023123456 ");

        assertThat(member.getStudentId()).isEqualTo("2023123456");
    }

    @Test
    void 카카오_회원_등록_시_필수값이_비어있으면_예외가_발생한다() {
        assertThatThrownBy(() -> Member.registerKakaoMember(
                "",
                "alice@example.com",
                "Alice",
                "https://example.com/profile.png"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 온보딩_완료_시_학번이_비어있거나_길이_제한을_넘으면_예외가_발생한다() {
        Member member = createMember();

        assertThatThrownBy(() -> member.completeOnboarding(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> member.completeOnboarding("20231234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Member createMember() {
        return Member.registerKakaoMember(
                "kakao-123",
                "alice@example.com",
                "Alice",
                "https://example.com/profile.png"
        );
    }
}
