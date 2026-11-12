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

    @Autowired
    private MemberSuspensionRepository suspensionRepository;

    @Test
    void 정지_종료_시각만_읽고_검색은_학번_이름_닉네임_부분_일치로_최신순이다() {
        Member alice = Member.registerKakaoMember("kakao-alice", "alice@example.com");
        alice.completeOnboarding("2023123456", "하승연");
        Member bob = Member.registerKakaoMember("kakao-bob", "bob@example.com");
        bob.completeOnboarding("2024000001", "김아코");
        memberRepository.save(alice);
        memberRepository.save(bob);
        java.time.LocalDateTime until = java.time.LocalDateTime.of(2026, 12, 1, 9, 0);
        alice.suspendUntil(until);
        memberRepository.flush();
        entityManager.clear();

        assertThat(memberRepository.findSuspendedUntilById(alice.getId())).contains(until);
        assertThat(memberRepository.findSuspendedUntilById(bob.getId())).isEmpty();
        var byStudentId = memberRepository.search(
                "2023",
                false,
                java.time.LocalDateTime.now(),
                org.springframework.data.domain.PageRequest.of(
                        0,
                        20,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "id")));
        assertThat(byStudentId.getContent()).extracting(Member::getId).containsExactly(alice.getId());
        var all = memberRepository.search(
                null,
                false,
                java.time.LocalDateTime.now(),
                org.springframework.data.domain.PageRequest.of(
                        0,
                        20,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "id")));
        assertThat(all.getContent()).extracting(Member::getId).containsExactly(bob.getId(), alice.getId());
        assertThat(memberRepository
                        .search(
                                "아코",
                                false,
                                java.time.LocalDateTime.now(),
                                org.springframework.data.domain.PageRequest.of(0, 20))
                        .getContent())
                .extracting(Member::getId)
                .containsExactly(bob.getId());

        // 정지 중 필터는 기준 시각보다 종료 시각이 늦은 회원만. 종료 시각이 지나면 빠진다.
        var page = org.springframework.data.domain.PageRequest.of(0, 20);
        assertThat(memberRepository.search(null, true, until.minusDays(1), page).getContent())
                .extracting(Member::getId)
                .containsExactly(alice.getId());
        assertThat(memberRepository.search(null, true, until, page).getContent())
                .isEmpty();
    }

    @Test
    void 장소_신고_정지_횟수를_회원별로_센다() {
        Member alice = memberRepository.save(Member.registerKakaoMember("kakao-alice", "alice@example.com"));
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 11, 13, 9, 0);
        suspensionRepository.save(MemberSuspension.start(
                alice.getId(),
                3,
                com.donggree.user.SuspensionSource.REPORT,
                com.donggree.user.ReportTargetType.PLACE,
                1L,
                null,
                99L,
                now));
        suspensionRepository.save(MemberSuspension.start(
                alice.getId(),
                3,
                com.donggree.user.SuspensionSource.REPORT,
                com.donggree.user.ReportTargetType.REVIEW,
                2L,
                null,
                99L,
                now));
        suspensionRepository.save(MemberSuspension.start(
                alice.getId(), 3, com.donggree.user.SuspensionSource.ADMIN, null, null, null, 99L, now));
        entityManager.flush();

        assertThat(suspensionRepository.countByMemberIdsAndReportType(
                        java.util.List.of(alice.getId()), com.donggree.user.ReportTargetType.PLACE))
                .singleElement()
                .satisfies(row -> assertThat(row[1]).isEqualTo(1L));
        assertThat(suspensionRepository.findByMemberIdOrderByIdDesc(alice.getId()))
                .hasSize(3);
    }
}
