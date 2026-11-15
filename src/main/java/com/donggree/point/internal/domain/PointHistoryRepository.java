package com.donggree.point.internal.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    /**
     * 적립 한 건을 넣는다. 같은 회원·사유·대상, 회원당 장소별 리뷰 적립, 장소별 첫 리뷰, 회원당 리포트 최초 확인
     * 유일 제약 중 하나라도 걸리면 아무것도 넣지 않는다. 이벤트가 다시 처리되거나 동시에 들어와도 한 번만 적립된다.
     *
     * @return 넣은 행 수(0 또는 1)
     */
    @Modifying
    @Query(
            value = "INSERT INTO point_history (member_id, reason, amount, target_id, created_at) "
                    + "VALUES (:memberId, :reason, :amount, :targetId, :createdAt) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int grant(
            @Param("memberId") Long memberId,
            @Param("reason") String reason,
            @Param("amount") int amount,
            @Param("targetId") Long targetId,
            @Param("createdAt") LocalDateTime createdAt);

    boolean existsByMemberIdAndReason(Long memberId, PointReason reason);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PointHistory h SET h.revokedAt = :at "
            + "WHERE h.targetId = :targetId AND h.reason IN :reasons AND h.revokedAt IS NULL")
    int revoke(
            @Param("targetId") Long targetId,
            @Param("reasons") Collection<PointReason> reasons,
            @Param("at") LocalDateTime at);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "UPDATE PointHistory h SET h.revokedAt = :at "
                    + "WHERE h.memberId = :memberId AND h.targetId = :targetId AND h.reason IN :reasons AND h.revokedAt IS NULL")
    int revokeOfMember(
            @Param("memberId") Long memberId,
            @Param("targetId") Long targetId,
            @Param("reasons") Collection<PointReason> reasons,
            @Param("at") LocalDateTime at);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PointHistory h SET h.revokedAt = NULL WHERE h.targetId = :targetId AND h.reason IN :reasons")
    int restore(@Param("targetId") Long targetId, @Param("reasons") Collection<PointReason> reasons);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PointHistory h SET h.revokedAt = NULL "
            + "WHERE h.memberId = :memberId AND h.targetId = :targetId AND h.reason IN :reasons")
    int restoreOfMember(
            @Param("memberId") Long memberId,
            @Param("targetId") Long targetId,
            @Param("reasons") Collection<PointReason> reasons);

    /** 회수되지 않은 적립의 합계. */
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM PointHistory h WHERE h.memberId = :memberId AND h.revokedAt IS NULL")
    long sumActiveAmount(@Param("memberId") Long memberId);

    List<PointHistory> findByMemberIdOrderByIdDesc(Long memberId, Limit limit);

    List<PointHistory> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long cursor, Limit limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PointHistory h WHERE h.memberId = :memberId")
    int deleteAllOfMember(@Param("memberId") Long memberId);
}
