package com.donggree.mystery.internal.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MysteryPlayerRepository extends JpaRepository<MysteryPlayer, Long> {
    @Modifying
    @Query(
            value = "INSERT INTO mystery_player (member_id) VALUES (:memberId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int createIfAbsent(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MysteryPlayer p WHERE p.memberId = :memberId")
    Optional<MysteryPlayer> lockByMemberId(@Param("memberId") Long memberId);
}
