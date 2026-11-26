package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubRequestRepository extends JpaRepository<ClubRequest, Long> {

    /** 두 관리자가 같은 요청을 동시에 처리하지 않도록 잠그고 읽는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ClubRequest r where r.id = :id")
    Optional<ClubRequest> findByIdForUpdate(@Param("id") Long id);

    /** 관리자 목록: 검토 중은 먼저 들어온 순(대기 순서). */
    List<ClubRequest> findByStatusOrderByIdAsc(ClubRequestStatus status);

    /** 관리자 목록: 처리된 요청은 최신순. */
    List<ClubRequest> findByStatusOrderByIdDesc(ClubRequestStatus status);

    List<ClubRequest> findAllByOrderByIdDesc();

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
