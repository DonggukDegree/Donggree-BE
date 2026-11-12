package com.donggree.place.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {

    /** 회원당 장소별 1건 확인. 숨긴 리뷰도 포함하고, 작성자가 지운 리뷰만 뺀다. */
    boolean existsByPlaceIdAndAuthorIdAndDeletedAtIsNull(Long placeId, Long authorId);

    Optional<PlaceReview> findByIdAndPlaceIdAndDeletedAtIsNull(Long id, Long placeId);

    Optional<PlaceReview> findByPlaceIdAndAuthorIdAndDeletedAtIsNull(Long placeId, Long authorId);

    /** 탈퇴 회원의 작성자 연결을 끊는다. 재가입 시 같은 회원 ID가 재활성화되므로 ID를 남기지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceReview r set r.authorId = null where r.authorId = :memberId")
    int clearAuthor(@Param("memberId") Long memberId);
}
