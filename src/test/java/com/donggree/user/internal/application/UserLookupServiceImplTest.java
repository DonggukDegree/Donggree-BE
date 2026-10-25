package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.domain.SchoolEmailAddress;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserLookupServiceImplTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final UserLookupServiceImpl service = new UserLookupServiceImpl(memberRepository);

    @Test
    void PDF_본인인증_또는_학교이메일_인증_회원은_인증된_학생이다() {
        Member pdfVerified = onboarded();
        pdfVerified.verifyIdentity();
        Member emailVerified = onboarded();
        emailVerified.verifySchoolEmail(SchoolEmailAddress.fromLocalPart("student"), LocalDateTime.now());
        given(memberRepository.findById(1L)).willReturn(Optional.of(pdfVerified));
        given(memberRepository.findById(2L)).willReturn(Optional.of(emailVerified));

        assertThat(service.isStudentVerified(1L)).isTrue();
        assertThat(service.isStudentVerified(2L)).isTrue();
    }

    @Test
    void 미인증이거나_없는_회원은_인증된_학생이_아니다() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(onboarded()));
        given(memberRepository.findById(2L)).willReturn(Optional.empty());

        assertThat(service.isStudentVerified(1L)).isFalse();
        assertThat(service.isStudentVerified(2L)).isFalse();
    }

    private static Member onboarded() {
        Member member = Member.registerKakaoMember("oauth", "test@example.invalid");
        member.completeOnboarding("2026000001", "테스트");
        return member;
    }
}
