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
