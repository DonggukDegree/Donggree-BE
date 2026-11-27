package com.donggree.club.internal.application;

import com.donggree.club.internal.application.command.ClubSaveCommand;
import com.donggree.club.internal.application.command.ClubSaveResult;
import com.donggree.club.internal.application.command.RecruitmentSaveCommand;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.ClubRequester;
import com.donggree.club.internal.domain.RecruitmentPlan;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.domain.enums.ClubRequestType;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.UserLookupService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동아리 신규 등록·정보 수정·모집 등록과 내 요청 다시 보내기(Command) 응용 서비스.
 *
 * <p>학생 인증 회원만 쓸 수 있다. 이 동아리의 직접 수정 권한이 있으면 정보 수정·모집 등록을 바로 반영하고,
 * 없으면 요청자 확인 정보(동아리와의 관계·연락처)와 함께 관리자 승인 요청으로 남긴다. 신규 등록은 항상 승인 요청이다.
 */
@Service
@RequiredArgsConstructor
public class ClubCommandService {

    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository recruitmentRepository;
    private final ClubRequestRepository requestRepository;
    private final ClubEditorRepository editorRepository;
    private final UserLookupService userLookupService;
    private final ClubClock clock;

    @Transactional
    public ClubSaveResult register(Long memberId, ClubSaveCommand command) {
        requireStudentVerified(memberId);
        submit(ClubRequest.register(
                memberId,
                ClubInputs.requester(command.requesterRole(), command.requesterContact()),
                ClubInputs.infoPayload(command)));
        return ClubSaveResult.pending(null);
    }

    @Transactional
    public ClubSaveResult edit(Long memberId, Long clubId, ClubSaveCommand command) {
        requireStudentVerified(memberId);
        Club club = findClub(clubId);
        ClubRequestPayload after = ClubInputs.infoPayload(command);
        if (editorRepository.canEdit(clubId, memberId)) {
            club.edit(after.profile(), clock.now());
            flushClub();
            return ClubSaveResult.direct(clubId);
        }
        requestEdit(memberId, club, ClubInputs.requester(command.requesterRole(), command.requesterContact()), after);
        return ClubSaveResult.pending(clubId);
    }

    /** 진행 중·예정 모집은 고쳐 쓰고, 모집이 없거나 마감됐으면 새 모집 건을 만든다. */
    @Transactional
    public ClubSaveResult saveRecruitment(Long memberId, Long clubId, RecruitmentSaveCommand command) {
        requireStudentVerified(memberId);
        Club club = findClub(clubId);
        RecruitmentPlan plan = ClubInputs.plan(command);
        if (editorRepository.canEdit(clubId, memberId)) {
            ClubRecruitment ongoing = ongoingRecruitment(clubId);
            if (ongoing == null) recruitmentRepository.save(ClubRecruitment.open(clubId, plan));
            else ongoing.revise(plan);
            return ClubSaveResult.direct(clubId);
        }
        requestRecruitment(
                memberId, club, ClubInputs.requester(command.requesterRole(), command.requesterContact()), plan);
        return ClubSaveResult.pending(clubId);
    }

    /** 검토 중·반려된 신규 등록·정보 수정 요청을 고쳐 다시 보낸다. 검토 중 요청은 취소되고 새 요청이 맨 뒤로 들어간다. */
    @Transactional
    public ClubSaveResult resubmitInfo(Long memberId, Long requestId, ClubSaveCommand command) {
        requireStudentVerified(memberId);
        ClubRequest previous = takeForResubmit(memberId, requestId, ClubRequestType.REGISTER, ClubRequestType.EDIT);
        ClubRequester requester = ClubInputs.requester(command.requesterRole(), command.requesterContact());
        ClubRequestPayload after = ClubInputs.infoPayload(command);
        if (previous.getType() == ClubRequestType.REGISTER) {
            submit(ClubRequest.register(memberId, requester, after));
            return ClubSaveResult.pending(null);
        }
        Club club = findClub(previous.getClubId());
        requestEdit(memberId, club, requester, after);
        return ClubSaveResult.pending(club.getId());
    }

    /** 검토 중·반려된 모집 요청을 고쳐 다시 보낸다. */
    @Transactional
    public ClubSaveResult resubmitRecruitment(Long memberId, Long requestId, RecruitmentSaveCommand command) {
        requireStudentVerified(memberId);
        ClubRequest previous = takeForResubmit(memberId, requestId, ClubRequestType.RECRUITMENT);
        ClubRequester requester = ClubInputs.requester(command.requesterRole(), command.requesterContact());
        RecruitmentPlan plan = ClubInputs.plan(command);
        Club club = findClub(previous.getClubId());
        requestRecruitment(memberId, club, requester, plan);
        return ClubSaveResult.pending(club.getId());
    }

    private void requestEdit(Long memberId, Club club, ClubRequester requester, ClubRequestPayload after) {
        ClubRequestPayload before = ClubRequestPayload.of(club.profile(), 0);
        submit(ClubRequest.edit(club, memberId, requester, before, after));
    }

    private void requestRecruitment(Long memberId, Club club, ClubRequester requester, RecruitmentPlan plan) {
        ClubRecruitment ongoing = ongoingRecruitment(club.getId());
        ClubRequestPayload before = ongoing == null ? null : ClubRequestPayload.of(ongoing.plan());
        submit(ClubRequest.recruitment(club, memberId, requester, before, ClubRequestPayload.of(plan)));
    }

    // 같은 동아리·종류의 검토 중 요청은 회원당 1건(DB 유일 인덱스가 동시 요청까지 막는다).
    private void submit(ClubRequest request) {
        if (request.getClubId() != null
                && requestRepository.existsByRequesterIdAndClubIdAndTypeAndStatus(
                        request.getRequesterId(), request.getClubId(), request.getType(), ClubRequestStatus.PENDING)) {
            throw new GeneralException(ClubErrorCode.DUPLICATE_PENDING);
        }
        try {
            requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(ClubErrorCode.DUPLICATE_PENDING);
        }
    }

    private ClubRequest takeForResubmit(Long memberId, Long requestId, ClubRequestType... types) {
        ClubRequest request = requestRepository
                .findById(requestId)
                .filter(found -> found.isRequestedBy(memberId))
                .orElseThrow(() -> new GeneralException(ClubErrorCode.REQUEST_NOT_FOUND));
        if (!List.of(types).contains(request.getType())) {
            throw new GeneralException(ClubErrorCode.REQUEST_TYPE_MISMATCH);
        }
        if (!request.resubmittable()) {
            throw new GeneralException(ClubErrorCode.REQUEST_CLOSED);
        }
        request.cancelForResubmit(clock.now());
        requestRepository.flush();
        return request;
    }

    private ClubRecruitment ongoingRecruitment(Long clubId) {
        LocalDateTime now = clock.now();
        return recruitmentRepository
                .findTopByClubIdOrderByIdDesc(clubId)
                .filter(latest -> latest.schedule().statusAt(now) != RecruitmentStatus.CLOSED)
                .orElse(null);
    }

    private void flushClub() {
        try {
            clubRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new GeneralException(ClubErrorCode.EDIT_CONFLICT);
        }
    }

    private Club findClub(Long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new GeneralException(ClubErrorCode.CLUB_NOT_FOUND));
    }

    private void requireStudentVerified(Long memberId) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(ClubErrorCode.VERIFICATION_REQUIRED);
        }
    }
}
