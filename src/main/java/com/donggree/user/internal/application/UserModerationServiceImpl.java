package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.AccessRestriction;
import com.donggree.global.auth.MemberAccessGuard;
import com.donggree.user.MemberAdminProfile;
import com.donggree.user.MemberSearchPage;
import com.donggree.user.MemberSuspensionRecord;
import com.donggree.user.ReportTargetType;
import com.donggree.user.SuspendMemberCommand;
import com.donggree.user.SuspensionSource;
import com.donggree.user.UserModerationService;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.domain.MemberSuspension;
import com.donggree.user.internal.domain.MemberSuspensionRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정지·해제와 관리자용 회원 조회. 정지 여부는 {@link MemberAccessGuard}로 JWT 필터·로그인·토큰 재발급에 알린다.
 * 정지 중 다시 정지하면 지금부터 새 일수로 바꾸고, 이전 정지 이력은 그 시각에 해제된 것으로 남긴다.
 */
@Service
@RequiredArgsConstructor
public class UserModerationServiceImpl implements UserModerationService, MemberAccessGuard {

    private final MemberRepository memberRepository;
    private final MemberSuspensionRepository suspensionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessRestriction> findRestriction(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        return memberRepository
                .findSuspendedUntilById(memberId)
                .filter(until -> until.isAfter(now))
                .map(until -> {
                    Optional<MemberSuspension> latest = suspensionRepository.findTopByMemberIdOrderByIdDesc(memberId);
                    return new AccessRestriction(
                            until,
                            latest.map(MemberSuspension::getDays).orElse(0),
                            latest.map(s -> s.getSource() == SuspensionSource.REPORT)
                                    .orElse(false));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, MemberAdminProfile> findAdminProfiles(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(ids).stream()
                .map(UserModerationServiceImpl::toProfile)
                .collect(Collectors.toMap(MemberAdminProfile::memberId, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberSearchPage searchMembers(String keyword, int page, int size) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.strip();
        var result = memberRepository.search(
                normalized, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        return new MemberSearchPage(
                result.getContent().stream()
                        .map(UserModerationServiceImpl::toProfile)
                        .toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
    }

    @Override
    @Transactional
    public MemberSuspensionRecord suspend(SuspendMemberCommand command) {
        Member member = memberRepository
                .findById(command.memberId())
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        MemberSuspension suspension;
        try {
            suspension = MemberSuspension.start(
                    member.getId(),
                    command.days(),
                    command.source(),
                    command.reportType(),
                    command.reportId(),
                    command.note(),
                    command.adminId(),
                    now);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(UserErrorCode.INVALID_SUSPENSION);
        }
        suspensionRepository
                .findTopByMemberIdOrderByIdDesc(member.getId())
                .filter(previous -> previous.isActive(now))
                .ifPresent(previous -> previous.lift(command.adminId(), now));
        member.suspendUntil(suspension.getEndsAt());
        return toRecord(suspensionRepository.save(suspension));
    }

    @Override
    @Transactional
    public void lift(Long memberId, Long adminId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        if (!member.isSuspended(now)) {
            return;
        }
        member.liftSuspension();
        suspensionRepository
                .findTopByMemberIdOrderByIdDesc(memberId)
                .filter(latest -> latest.isActive(now))
                .ifPresent(latest -> latest.lift(adminId, now));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberSuspensionRecord> findSuspensions(Long memberId) {
        return suspensionRepository.findByMemberIdOrderByIdDesc(memberId).stream()
                .map(UserModerationServiceImpl::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countReportSuspensions(Collection<Long> memberIds, ReportTargetType reportType) {
        List<Long> ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return suspensionRepository.countByMemberIdsAndReportType(ids, reportType).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private static MemberAdminProfile toProfile(Member member) {
        return new MemberAdminProfile(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getStudentId(),
                member.getRole().name(),
                member.getCreatedAt(),
                member.getSuspendedUntil());
    }

    private static MemberSuspensionRecord toRecord(MemberSuspension suspension) {
        return new MemberSuspensionRecord(
                suspension.getId(),
                suspension.getDays(),
                suspension.getSource(),
                suspension.getReportType(),
                suspension.getReportId(),
                suspension.getNote(),
                suspension.getStartsAt(),
                suspension.getEndsAt(),
                suspension.getCreatedBy(),
                suspension.getLiftedAt(),
                suspension.getLiftedBy());
    }
}
