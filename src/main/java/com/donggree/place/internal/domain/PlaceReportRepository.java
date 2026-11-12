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

    List<PlaceReport> findByPlaceIdOrderByIdAsc(Long placeId);

    /** 관리자가 장소를 숨기면 그 장소의 처리 대기 신고를 모두 처리 완료로 바꾼다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceReport r set r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.RESOLVED,"
            + " r.processedAt = :processedAt, r.updatedAt = :processedAt"
            + " where r.placeId = :placeId"
            + " and r.status = com.donggree.place.internal.domain.enums.PlaceReportStatus.PENDING")
    int resolvePending(@Param("placeId") Long placeId, @Param("processedAt") LocalDateTime processedAt);

    /** 탈퇴 회원의 신고자 연결을 끊는다. 재가입 시 같은 회원 ID가 재활성화되므로 ID를 남기지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlaceReport r set r.reporterId = null where r.reporterId = :memberId")
    int clearReporter(@Param("memberId") Long memberId);
}
