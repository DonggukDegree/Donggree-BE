package com.donggree.club.internal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubFavoriteRepository extends JpaRepository<ClubFavorite, ClubFavorite.Key> {

    /** 회원이 찜한 동아리 ID(최근 찜한 순). */
    @Query("select f.key.clubId from ClubFavorite f where f.key.memberId = :memberId "
            + "order by f.createdAt desc, f.key.clubId desc")
    List<Long> findClubIds(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ClubFavorite f where f.key.memberId = :memberId")
    int deleteByMember(@Param("memberId") Long memberId);
}
