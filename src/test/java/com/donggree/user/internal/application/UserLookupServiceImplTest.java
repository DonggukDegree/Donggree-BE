package com.donggree.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.user.MemberProfile;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.domain.SchoolEmailAddress;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void 회원_표시_정보는_조회된_활동_회원만_ID별로_돌려준다() {
        Member member = onboarded();
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(member));

        Map<Long, MemberProfile> profiles = service.findProfiles(Arrays.asList(1L, null, 2L, 1L));

        assertThat(profiles).containsOnlyKeys(1L);
        assertThat(profiles.get(1L)).isEqualTo(new MemberProfile(1L, "테스트", null));
        assertThat(service.findProfiles(Arrays.asList((Long) null))).isEmpty();
        verify(memberRepository, never()).findAllById(List.of());
    }

    private static Member onboarded() {
        Member member = Member.registerKakaoMember("oauth", "test@example.invalid");
        member.completeOnboarding("2026000001", "테스트");
        return member;
    }
}
