package com.donggree.club.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubEditorRepository extends JpaRepository<ClubEditor, ClubEditor.Key> {

    default boolean canEdit(Long clubId, Long memberId) {
        return memberId != null && existsById(new ClubEditor.Key(clubId, memberId));
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ClubEditor e where e.key.memberId = :memberId")
    int deleteByMember(@Param("memberId") Long memberId);
}
