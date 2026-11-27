package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubRequestRepository extends JpaRepository<ClubRequest, Long> {

    /** 내 요청 내역(최신순). */
    List<ClubRequest> findByRequesterIdOrderByIdDesc(Long requesterId);

    boolean existsByRequesterIdAndClubIdAndTypeAndStatus(
            Long requesterId, Long clubId, ClubRequestType type, ClubRequestStatus status);

    /** 보유 기간이 지난 요청자 연락처·동아리와의 관계를 비운다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ClubRequest r set r.requesterContact = null, r.requesterRole = null "
            + "where r.createdAt < :before and (r.requesterContact is not null or r.requesterRole is not null)")
    int clearRequesterInfoBefore(@Param("before") LocalDateTime before);

    /** 탈퇴 회원의 요청은 남기고 요청자 연결과 연락처·관계만 비운다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ClubRequest r set r.requesterId = null, r.requesterContact = null, r.requesterRole = null "
            + "where r.requesterId = :memberId")
    int clearRequester(@Param("memberId") Long memberId);
}
