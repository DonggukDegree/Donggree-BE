package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.dto.UserInfoResponse;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    void 온보딩을_완료하면_학번과_이름이_저장된다() {
        Long memberId = 1L;
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.existsByStudentId("2023123456")).willReturn(false);

        userService.completeOnboarding(memberId, "2023123456", "하승연");

        assertThat(member.getStudentId()).isEqualTo("2023123456");
        assertThat(member.getName()).isEqualTo("하승연");
        assertThat(member.getNickname()).isEqualTo("하승연");
        assertThat(member.hasCompletedOnboarding()).isTrue();
    }

    @Test
    void 존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외가_발생한다() {
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.completeOnboarding(memberId, "2023123456", "하승연"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 이미_온보딩이_완료된_회원이면_ALREADY_ONBOARDED_예외가_발생한다() {
        Long memberId = 1L;
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        member.completeOnboarding("2023123456", "하승연");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> userService.completeOnboarding(memberId, "2023999999", "홍길동"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.ALREADY_ONBOARDED));
    }

    @Test
    void 이미_사용_중인_학번이면_DUPLICATE_STUDENT_ID_예외가_발생한다() {
        Long memberId = 1L;
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.existsByStudentId("2023123456")).willReturn(true);

        assertThatThrownBy(() -> userService.completeOnboarding(memberId, "2023123456", "하승연"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.DUPLICATE_STUDENT_ID));
    }

    // --- updateUserInfo 테스트 ---

    @Test
    void 닉네임을_변경하면_수정된_프로필이_반환된다() {
        Long memberId = 1L;
        Member member = createOnboardedMember(memberId, "2023123456", "하승연");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        UserInfoResponse response = userService.updateUserInfo(memberId, "2023123456", "하승연", "동동이");

        assertThat(response.nickname()).isEqualTo("동동이");
        assertThat(response.studentId()).isEqualTo("2023123456");
        assertThat(response.name()).isEqualTo("하승연");
        assertThat(response.identityVerified()).isFalse();
    }

    @Test
    void 수정_시_존재하지_않는_회원이면_MEMBER_NOT_FOUND_예외가_발생한다() {
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserInfo(memberId, "2023123456", "하승연", "동동이"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 온보딩_미완료_상태에서_수정하면_NOT_ONBOARDED_예외가_발생한다() {
        Long memberId = 1L;
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> userService.updateUserInfo(memberId, "2023123456", "하승연", "동동이"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.NOT_ONBOARDED));
    }

    @Test
    void 본인_인증_완료_후_학번_변경_시도하면_IDENTITY_ALREADY_VERIFIED_예외가_발생한다() {
        Long memberId = 1L;
        Member member = createOnboardedMember(memberId, "2023123456", "하승연");
        member.verifyIdentity();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> userService.updateUserInfo(memberId, "2023999999", "하승연", "동동이"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.IDENTITY_ALREADY_VERIFIED));
    }

    @Test
    void 본인_인증_완료_후_이름_변경_시도하면_IDENTITY_ALREADY_VERIFIED_예외가_발생한다() {
        Long memberId = 1L;
        Member member = createOnboardedMember(memberId, "2023123456", "하승연");
        member.verifyIdentity();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> userService.updateUserInfo(memberId, "2023123456", "홍길동", "동동이"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.IDENTITY_ALREADY_VERIFIED));
    }

    @Test
    void 수정_시_다른_회원과_학번이_중복되면_DUPLICATE_STUDENT_ID_예외가_발생한다() {
        Long memberId = 1L;
        Member member = createOnboardedMember(memberId, "2023123456", "하승연");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.existsByStudentIdAndIdNot("2023999999", memberId)).willReturn(true);

        assertThatThrownBy(() -> userService.updateUserInfo(memberId, "2023999999", "하승연", "동동이"))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(UserErrorCode.DUPLICATE_STUDENT_ID));
    }

    /**
     * 온보딩이 완료된 Member를 생성하고 id를 설정한다 (JPA 영속화 없이 테스트용).
     */
    private Member createOnboardedMember(Long memberId, String studentId, String name) {
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        member.completeOnboarding(studentId, name);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
