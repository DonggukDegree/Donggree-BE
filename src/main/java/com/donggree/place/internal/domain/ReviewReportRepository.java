package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReporterIdAndStatus(Long reviewId, Long reporterId, PlaceReportStatus status);

    /** 처리 대기 신고 수. 회원당 리뷰별 대기 1건이라 서로 다른 신고자 수와 같다. */
    long countByReviewIdAndStatus(Long reviewId, PlaceReportStatus status);

    List<ReviewReport> findByReviewIdOrderByIdAsc(Long reviewId);

    /** 관리자 목록. 상태별(null이면 전체) 최신순. */
    @Query("select r from ReviewReport r where :status is null or r.status = :status")
    org.springframework.data.domain.Page<ReviewReport> findForAdmin(
            @Param("status") PlaceReportStatus status, org.springframework.data.domain.Pageable pageable);

    /** 회원이 쓴 리뷰에 들어온 신고 수(작성자별, 상태 무관). */
    @Query("select r.authorId, count(rr) from ReviewReport rr, PlaceReview r"
            + " where rr.reviewId = r.id and r.authorId in :authorIds group by r.authorId")
    List<Object[]> countReceivedByAuthorIds(@Param("authorIds") java.util.Collection<Long> authorIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReviewReport r set r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.RESOLVED,"
            + " r.processedAt = :processedAt, r.updatedAt = :processedAt"
            + " where r.reviewId = :reviewId"
            + " and r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.PENDING")
    int resolvePending(@Param("reviewId") Long reviewId, @Param("processedAt") LocalDateTime processedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReviewReport r set r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.REJECTED,"
            + " r.processedAt = :processedAt, r.updatedAt = :processedAt"
            + " where r.reviewId = :reviewId"
            + " and r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.PENDING")
    int rejectPending(@Param("reviewId") Long reviewId, @Param("processedAt") LocalDateTime processedAt);

    /** 탈퇴 회원의 신고자 연결을 끊는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReviewReport r set r.reporterId = null where r.reporterId = :memberId")
    int clearReporter(@Param("memberId") Long memberId);
}
