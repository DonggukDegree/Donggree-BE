package com.donggree.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.global.config.JpaAuditingConfig;
import com.donggree.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// QueryDslConfig는 CourseClassificationRepository의 QueryDSL 커스텀 구현(JPAQueryFactory 의존)을
// @DataJpaTest 컨텍스트에 제공하기 위해 import한다. (Spring Data가 전체 리포지토리를 초기화하므로 필요)
@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

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
        member.completeOnboarding("2023123456", "하승연");
        memberRepository.save(member);

        boolean exists = memberRepository.existsByStudentId("2023123456");

        assertThat(exists).isTrue();
    }

    // --- soft-delete 필터링 테스트 ---

    @Test
    void 탈퇴한_회원은_findByOauthId로_조회되지_않는다() {
        Member member = Member.registerKakaoMember("kakao-deleted", "deleted@example.com");
        memberRepository.save(member);
        member.withdraw();
        memberRepository.flush();
        entityManager.clear();

        var result = memberRepository.findByOauthId("kakao-deleted");

        assertThat(result).isEmpty();
    }

    @Test
    void 탈퇴한_회원은_findById로_조회되지_않는다() {
        Member member = Member.registerKakaoMember("kakao-deleted", "deleted@example.com");
        memberRepository.save(member);
        Long memberId = member.getId();
        member.withdraw();
        memberRepository.flush();
        entityManager.clear();

        var result = memberRepository.findById(memberId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByOauthIdIncludingDeleted로_탈퇴한_회원을_조회할_수_있다() {
        Member member = Member.registerKakaoMember("kakao-deleted", "deleted@example.com");
        memberRepository.save(member);
        member.withdraw();
        memberRepository.flush();
        entityManager.clear();

        var result = memberRepository.findByOauthIdIncludingDeleted("kakao-deleted");

        assertThat(result).isPresent();
        assertThat(result.get().isDeleted()).isTrue();
    }
}
