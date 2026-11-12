package com.donggree.user.internal.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    boolean existsBySchoolEmailAndIdNot(String schoolEmail, Long id);

    Optional<Member> findByOauthId(String oauthId);

    boolean existsByStudentId(String studentId);

    boolean existsByStudentIdAndIdNot(String studentId, Long id);

    /**
     * @SQLRestriction을 우회하여 탈퇴 회원을 포함한 전체 회원 중 oauthId로 조회한다.
     * OAuth 재로그인 시 탈퇴 회원의 재활성화 여부를 판단하기 위해 사용한다.
     */
    /** 정지 확인용. 요청마다 불리므로 정지 종료 시각만 읽는다(탈퇴 회원은 조회되지 않음). */
    @Query("select m.suspendedUntil from Member m where m.id = :id")
    Optional<java.time.LocalDateTime> findSuspendedUntilById(@Param("id") Long id);

    /**
     * 관리자 회원 검색. 학번·이름·닉네임 부분 일치, 가입 최신순.
     *
     * @param suspendedOnly true면 now 기준 정지 중인 회원만
     */
    @Query("select m from Member m where (:keyword is null or m.studentId like concat('%', :keyword, '%')"
            + " or m.name like concat('%', :keyword, '%') or m.nickname like concat('%', :keyword, '%'))"
            + " and (:suspendedOnly = false or m.suspendedUntil > :now)")
    org.springframework.data.domain.Page<Member> search(
            @Param("keyword") String keyword,
            @Param("suspendedOnly") boolean suspendedOnly,
            @Param("now") java.time.LocalDateTime now,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT * FROM member WHERE oauth_id = :oauthId", nativeQuery = true)
    Optional<Member> findByOauthIdIncludingDeleted(@Param("oauthId") String oauthId);
}
