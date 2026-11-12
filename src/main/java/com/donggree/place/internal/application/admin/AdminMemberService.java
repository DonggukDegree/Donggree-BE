package com.donggree.place.internal.application.admin;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberRow;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.domain.ReviewReportRepository;
import com.donggree.user.MemberAdminProfile;
import com.donggree.user.MemberSearchPage;
import com.donggree.user.MemberSuspensionRecord;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspendMemberCommand;
import com.donggree.user.SuspensionSource;
import com.donggree.user.UserModerationService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 대시보드의 회원 관리. 회원 검색·상세(이름·학번·단과대·학과·받은 신고·정지 이력)와 신고와 별개인 관리자 정지·해제.
 * 받은 신고는 그 회원 리뷰에 들어온 신고 수와 장소 신고 처리로 정지된 횟수를 더한다(반복 위반 판단용, 자동 가중 없음).
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    public static final int PAGE_SIZE = 20;

    private final UserModerationService moderationService;
    private final AdminMemberReader memberReader;
    private final ReviewReportRepository reviewReportRepository;

    @Transactional(readOnly = true)
    public Page<MemberRow> searchMembers(String keyword, int page) {
        MemberSearchPage result = moderationService.searchMembers(keyword, page, PAGE_SIZE);
        Map<Long, AdminMemberSummary> summaries = memberReader.toSummaries(result.members());
        List<Long> ids =
                result.members().stream().map(MemberAdminProfile::memberId).toList();
        Map<Long, Long> received = receivedReportCounts(ids);
        return new Page<>(
                result.members().stream()
                        .map(profile -> new MemberRow(
                                summaries.get(profile.memberId()), received.getOrDefault(profile.memberId(), 0L)))
                        .toList(),
                result.totalCount(),
                result.page(),
                result.size());
    }

    @Transactional(readOnly = true)
    public MemberDetail getMember(Long memberId) {
        MemberAdminProfile profile =
                moderationService.findAdminProfiles(List.of(memberId)).get(memberId);
        if (profile == null) {
            throw new GeneralException(
                    com.donggree.place.internal.application.exception.PlaceErrorCode.MEMBER_NOT_FOUND);
        }
        AdminMemberSummary summary = memberReader.toSummaries(List.of(profile)).get(memberId);
        long reviewReports = reviewReportCounts(List.of(memberId)).getOrDefault(memberId, 0L);
        long placeSuspensions = moderationService
                .countReportSuspensions(List.of(memberId), ReportTargetType.PLACE)
                .getOrDefault(memberId, 0L);
        return new MemberDetail(
                summary,
                profile.role(),
                profile.joinedAt(),
                reviewReports,
                placeSuspensions,
                reviewReports + placeSuspensions,
                moderationService.findSuspensions(memberId));
    }

    /** 신고와 별개인 관리자 판단 정지. 지금부터 days일, 정지 중이면 새 일수로 바꾼다. */
    @Transactional
    public MemberSuspensionRecord suspend(Long memberId, int days, String note, Long adminId) {
        return moderationService.suspend(
                new SuspendMemberCommand(memberId, days, SuspensionSource.ADMIN, null, null, note, adminId));
    }

    @Transactional
    public void lift(Long memberId, Long adminId) {
        moderationService.lift(memberId, adminId);
    }

    private Map<Long, Long> receivedReportCounts(Collection<Long> memberIds) {
        Map<Long, Long> reviewReports = reviewReportCounts(memberIds);
        Map<Long, Long> placeSuspensions = moderationService.countReportSuspensions(memberIds, ReportTargetType.PLACE);
        return memberIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> reviewReports.getOrDefault(id, 0L) + placeSuspensions.getOrDefault(id, 0L),
                        (first, second) -> first));
    }

    private Map<Long, Long> reviewReportCounts(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return reviewReportRepository.countReceivedByAuthorIds(memberIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
