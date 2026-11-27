package com.donggree.club.internal.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubEditorRepository extends JpaRepository<ClubEditor, ClubEditor.Key> {

    default boolean canEdit(Long clubId, Long memberId) {
        return memberId != null && existsById(new ClubEditor.Key(clubId, memberId));
    }

    /** 동아리들의 직접 수정 권한 회원(부여 순). */
    @Query("select e from ClubEditor e where e.key.clubId in :clubIds order by e.grantedAt asc, e.key.memberId asc")
    List<ClubEditor> findByClubIds(@Param("clubIds") Collection<Long> clubIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ClubEditor e where e.key.memberId = :memberId")
    int deleteByMember(@Param("memberId") Long memberId);
}
