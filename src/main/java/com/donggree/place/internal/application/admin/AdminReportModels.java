package com.donggree.place.internal.application.admin;

import com.donggree.place.internal.domain.MenuItem;
import com.donggree.place.internal.domain.enums.PlaceCategory;
import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import com.donggree.place.internal.domain.enums.PlaceReviewStatus;
import com.donggree.place.internal.domain.enums.PlaceStatus;
import com.donggree.place.internal.domain.enums.PlaceSubCategory;
import com.donggree.place.internal.domain.enums.PlaceTag;
import com.donggree.place.internal.domain.enums.ReviewReportReason;
import com.donggree.user.MemberSuspensionRecord;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 통합 신고 대시보드 읽기 모델 모음. 회원 행(AdminMemberSummary)이 null이면 탈퇴한 회원이다. */
public final class AdminReportModels {

    private AdminReportModels() {}

    public record Page<T>(List<T> items, long totalCount, int page, int size) {}

    public record PlaceReportItem(
            Long reportId,
            PlaceReportReason reason,
            String description,
            PlaceReportStatus status,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            AdminMemberSummary reporter,
            Long placeId,
            String placeName,
            PlaceCategory category,
            PlaceStatus placeStatus,
            long pendingCount) {}

    public record ReviewReportItem(
            Long reportId,
            ReviewReportReason reason,
            String description,
            PlaceReportStatus status,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            AdminMemberSummary reporter,
            Long reviewId,
            int rating,
            String content,
            PlaceReviewStatus reviewStatus,
            boolean reviewDeleted,
            AdminMemberSummary author,
            Long placeId,
            String placeName,
            long pendingCount) {}

    /** 장소 신고 상세. 같은 장소의 신고 전체와 최근 수정 이력(수정자 포함)을 함께 본다. */
    public record PlaceReportDetail(
            PlaceReportItem report,
            PlaceSnapshot place,
            AdminMemberSummary registrant,
            List<EditHistoryItem> editHistories,
            List<PlaceReportItem> placeReports) {}

    public record PlaceSnapshot(
            Long id,
            String name,
            PlaceCategory category,
            PlaceSubCategory subCategory,
            PlaceStatus status,
            String businessHours,
            String description,
            List<MenuItem> menus,
            List<PlaceTag> tags,
            LocalDateTime createdAt,
            LocalDateTime infoUpdatedAt) {}

    public record InfoSnapshot(String businessHours, String description, List<MenuItem> menus, List<PlaceTag> tags) {}

    public record EditHistoryItem(
            Long historyId,
            AdminMemberSummary editor,
            LocalDateTime editedAt,
            InfoSnapshot before,
            InfoSnapshot after) {}

    /** 리뷰 신고 상세. 같은 리뷰의 신고 전체를 함께 본다. */
    public record ReviewReportDetail(ReviewReportItem report, List<ReviewReportItem> reviewReports) {}

    /**
     * 회원 상세. 받은 신고는 그 회원 리뷰에 들어온 신고 수와, 장소 신고 처리로 정지된 횟수를 더한다.
     */
    public record MemberDetail(
            AdminMemberSummary member,
            String role,
            LocalDateTime joinedAt,
            long receivedReviewReportCount,
            long placeReportSuspensionCount,
            long receivedReportCount,
            List<MemberSuspensionRecord> suspensions) {}

    public record MemberRow(AdminMemberSummary member, long receivedReportCount) {}
}
