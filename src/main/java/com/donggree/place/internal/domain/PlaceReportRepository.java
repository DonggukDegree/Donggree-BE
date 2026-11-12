package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceReportRepository extends JpaRepository<PlaceReport, Long> {

    boolean existsByPlaceIdAndReporterIdAndStatus(Long placeId, Long reporterId, PlaceReportStatus status);

    /** 처리 대기 신고 수. 회원당 장소별 대기 1건이라 서로 다른 신고자 수와 같다. */
    long countByPlaceIdAndStatus(Long placeId, PlaceReportStatus status);

    List<PlaceReport> findByPlaceIdOrderByIdAsc(Long placeId);

    /** 관리자 목록. 상태별(null이면 전체) 최신순. */
    @Query("select r from PlaceReport r where :status is null or r.status = :status")
    org.springframework.data.domain.Page<PlaceReport> findForAdmin(
            @Param("status") PlaceReportStatus status, org.springframework.data.domain.Pageable pageable);

    /** 관리자가 장소를 숨기면 그 장소의 처리 대기 신고를 모두 처리 완료로 바꾼다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceReport r set r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.RESOLVED,"
            + " r.processedAt = :processedAt, r.updatedAt = :processedAt"
            + " where r.placeId = :placeId"
            + " and r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.PENDING")
    int resolvePending(@Param("placeId") Long placeId, @Param("processedAt") LocalDateTime processedAt);

    /** 관리자가 숨김을 해제하면 그 장소의 처리 대기 신고를 기각으로 바꿔 자동 숨김 집계를 다시 시작한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceReport r set r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.REJECTED,"
            + " r.processedAt = :processedAt, r.updatedAt = :processedAt"
            + " where r.placeId = :placeId"
            + " and r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.PENDING")
    int rejectPending(@Param("placeId") Long placeId, @Param("processedAt") LocalDateTime processedAt);

    /** 탈퇴 회원의 신고자 연결을 끊는다. 재가입 시 같은 회원 ID가 재활성화되므로 ID를 남기지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceReport r set r.reporterId = null where r.reporterId = :memberId")
    int clearReporter(@Param("memberId") Long memberId);
}
