package com.donggree.place.internal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceEditHistoryRepository extends JpaRepository<PlaceEditHistory, Long> {

    List<PlaceEditHistory> findByPlaceIdOrderByEditedAtAsc(Long placeId);

    /** 탈퇴 회원의 수정자 연결을 끊는다. 재가입 시 같은 회원 ID가 재활성화되므로 ID를 남기지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceEditHistory h set h.editorId = null where h.editorId = :memberId")
    int clearEditor(@Param("memberId") Long memberId);
}
