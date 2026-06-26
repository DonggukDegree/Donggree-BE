package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.user.internal.domain.enums.Role;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void 카카오_회원_등록_시_기본_권한과_온보딩_미완료_상태로_초기화한다() {
        Member member = createMember();

        assertThat(member.getStudentId()).isNull();
        assertThat(member.getName()).isNull();
        assertThat(member.getNickname()).isNull();
        assertThat(member.getEmail()).isEqualTo("alice@example.com");
        assertThat(member.getOauthId()).isEqualTo("kakao-123");
        assertThat(member.getProfileUrl()).isNull();
        assertThat(member.getRole()).isEqualTo(Role.STUDENT);
        assertThat(member.hasCompletedOnboarding()).isFalse();
    }

    @Test
    void 카카오_회원_등록_시_문자열_앞뒤_공백을_제거한다() {
        Member member = Member.registerKakaoMember(" kakao-123 ", " alice@example.com ");

        assertThat(member.getOauthId()).isEqualTo("kakao-123");
        assertThat(member.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void 온보딩_완료_시_학번과_이름을_저장하고_닉네임을_이름으로_초기화한다() {
        Member member = createMember();

        member.completeOnboarding("2023123456", "하승연");

        assertThat(member.getStudentId()).isEqualTo("2023123456");
        assertThat(member.getName()).isEqualTo("하승연");
        assertThat(member.getNickname()).isEqualTo("하승연");
        assertThat(member.hasCompletedOnboarding()).isTrue();
    }

    @Test
    void 온보딩_완료_시_문자열_앞뒤_공백을_제거한다() {
        Member member = createMember();

        member.completeOnboarding(" 2023123456 ", " 하승연 ");

        assertThat(member.getStudentId()).isEqualTo("2023123456");
        assertThat(member.getName()).isEqualTo("하승연");
    }

    @Test
    void 이미_온보딩_완료된_상태에서_재호출하면_예외가_발생한다() {
        Member member = createMember();
        member.completeOnboarding("2023123456", "하승연");

        assertThatThrownBy(() -> member.completeOnboarding("2023999999", "홍길동"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 카카오_회원_등록_시_필수값이_비어있으면_예외가_발생한다() {
        assertThatThrownBy(() -> Member.registerKakaoMember("", "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 온보딩_완료_시_학번이_비어있거나_길이_제한을_넘으면_예외가_발생한다() {
        Member member = createMember();

        assertThatThrownBy(() -> member.completeOnboarding("", "하승연")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> member.completeOnboarding("20231234567", "하승연"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 온보딩_완료_시_이름이_비어있거나_길이_제한을_넘으면_예외가_발생한다() {
        Member member = createMember();

        assertThatThrownBy(() -> member.completeOnboarding("2023123456", ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> member.completeOnboarding("2023123456", "여섯글자이름임"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 리프레시_토큰을_저장한다() {
        Member member = createMember();

        member.updateRefreshToken("refresh-token-value");

        assertThat(member.getRefreshToken()).isEqualTo("refresh-token-value");
    }

    @Test
    void 리프레시_토큰을_삭제하면_null이_된다() {
        Member member = createMember();
        member.updateRefreshToken("refresh-token-value");

        member.clearRefreshToken();

        assertThat(member.getRefreshToken()).isNull();
    }

    // --- withdraw 테스트 ---

    @Test
    void 탈퇴하면_deletedAt이_기록되고_refreshToken과_studentId가_초기화된다() {
        Member member = createMember();
        member.completeOnboarding("2023123456", "하승연");
        member.updateRefreshToken("refresh-token-value");

        member.withdraw();

        assertThat(member.getDeletedAt()).isNotNull();
        assertThat(member.getRefreshToken()).isNull();
        assertThat(member.getStudentId()).isNull();
        assertThat(member.isDeleted()).isTrue();
    }

    @Test
    void 이미_탈퇴한_회원이_다시_탈퇴하면_예외가_발생한다() {
        Member member = createMember();
        member.withdraw();

        assertThatThrownBy(member::withdraw).isInstanceOf(IllegalStateException.class);
    }

    // --- reactivate 테스트 ---

    @Test
    void 재활성화하면_deletedAt이_해제되고_프로필이_초기_상태로_복원된다() {
        Member member = createMember();
        member.completeOnboarding("2023123456", "하승연");
        member.verifyIdentity();
        member.withdraw();

        member.reactivate();

        assertThat(member.getDeletedAt()).isNull();
        assertThat(member.isDeleted()).isFalse();
        assertThat(member.getStudentId()).isNull();
        assertThat(member.getName()).isNull();
        assertThat(member.getNickname()).isNull();
        assertThat(member.getProfileUrl()).isNull();
        assertThat(member.isIdentityVerified()).isFalse();
    }

    @Test
    void 재활성화_후_온보딩을_다시_진행할_수_있다() {
        Member member = createMember();
        member.completeOnboarding("2023123456", "하승연");
        member.withdraw();
        member.reactivate();

        assertThat(member.hasCompletedOnboarding()).isFalse();

        member.completeOnboarding("2023999999", "홍길동");

        assertThat(member.getStudentId()).isEqualTo("2023999999");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.hasCompletedOnboarding()).isTrue();
    }

    @Test
    void 활성_상태의_회원을_재활성화하면_예외가_발생한다() {
        Member member = createMember();

        assertThatThrownBy(member::reactivate).isInstanceOf(IllegalStateException.class);
    }

    private Member createMember() {
        return Member.registerKakaoMember("kakao-123", "alice@example.com");
    }
}
