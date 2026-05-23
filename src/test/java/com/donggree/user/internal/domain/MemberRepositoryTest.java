package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 카카오_oauth_id로_회원을_조회한다() {
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        memberRepository.save(member);

        var foundMember = memberRepository.findByOauthId("kakao-123");

        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void 학번이_저장된_회원이_있는지_확인한다() {
        Member member = Member.registerKakaoMember("kakao-123", "alice@example.com");
        member.completeOnboarding("2023123456");
        memberRepository.save(member);

        boolean exists = memberRepository.existsByStudentId("2023123456");

        assertThat(exists).isTrue();
    }
}
