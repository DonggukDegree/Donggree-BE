package com.donggree.user.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByOauthId(String oauthId);

    boolean existsByStudentId(String studentId);

    boolean existsByStudentIdAndIdNot(String studentId, Long id);

    /**
     * @SQLRestriction을 우회하여 탈퇴 회원을 포함한 전체 회원 중 oauthId로 조회한다.
     * OAuth 재로그인 시 탈퇴 회원의 재활성화 여부를 판단하기 위해 사용한다.
     */
    @Query(value = "SELECT * FROM member WHERE oauth_id = :oauthId", nativeQuery = true)
    Optional<Member> findByOauthIdIncludingDeleted(@Param("oauthId") String oauthId);
}
