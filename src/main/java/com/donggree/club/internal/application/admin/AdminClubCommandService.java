package com.donggree.club.internal.application.admin;

import com.donggree.club.event.ClubRequestApprovedEvent;
import com.donggree.club.internal.application.ClubClock;
import com.donggree.club.internal.application.ClubInputs;
import com.donggree.club.internal.application.ClubPhotoBinder;
import com.donggree.club.internal.application.command.ClubSaveCommand;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.Club;
import com.donggree.club.internal.domain.ClubEditor;
import com.donggree.club.internal.domain.ClubEditorRepository;
import com.donggree.club.internal.domain.ClubRecruitment;
import com.donggree.club.internal.domain.ClubRecruitmentRepository;
import com.donggree.club.internal.domain.ClubRepository;
import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.ClubRequestRepository;
import com.donggree.club.internal.domain.RecruitmentPlan;
import com.donggree.club.internal.domain.enums.ClubRegistration;
import com.donggree.club.internal.domain.enums.RecruitmentStatus;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 동아리 요청 승인·반려, 임시 등록·정보 수정, 직접 수정 권한 회수(Command) 응용 서비스.
 *
 * <p>승인하면 요청 값을 바로 반영하고, 임시 등록 동아리는 정식 등록으로 바꾸며, 요청자에게 그 동아리의 직접 수정 권한을 준다.
 * 적립 대상(최초 등록·새 기수 모집·기수 갱신)은 승인 이벤트로 알리고 포인트 모듈이 같은 트랜잭션에서 적립한다.
 */
@Service
@RequiredArgsConstructor
public class AdminClubCommandService {

    private final ClubRequestRepository requestRepository;
    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository recruitmentRepository;
    private final ClubEditorRepository editorRepository;
    private final ClubPhotoBinder photoBinder;
    private final ApplicationEventPublisher eventPublisher;
    private final ClubClock clock;

    @Transactional
    public void approve(Long requestId) {
        ClubRequest request = lockPending(requestId);
        LocalDateTime now = clock.now();
        Club club;
        boolean firstRegistration;
        Integer recruitedGeneration = null;
        Integer updatedGeneration = null;
        switch (request.getType()) {
            case REGISTER -> {
                club = clubRepository.save(Club.register(request.getAfter().profile(), ClubRegistration.OFFICIAL, now));
                photoBinder.bind(club.getId(), request.getAfter().photos());
                firstRegistration = true;
            }
            case EDIT -> {
                club = findClub(request.getClubId());
                Integer previous = club.getGeneration();
                club.edit(request.getAfter().profile(), now);
                photoBinder.bind(club.getId(), request.getAfter().photos());
                Integer current = club.getGeneration();
                if (current != null && (previous == null || current > previous)) updatedGeneration = current;
                firstRegistration = club.becomeOfficial();
            }
            case RECRUITMENT -> {
                club = findClub(request.getClubId());
                recruitedGeneration =
                        applyRecruitment(club.getId(), request.getAfter().recruitment(), now);
                firstRegistration = club.becomeOfficial();
            }
            default -> throw new IllegalStateException("알 수 없는 요청 종류입니다");
        }
        request.approve(club.getId(), now);
        Long memberId = request.getRequesterId();
        if (memberId == null) return; // 탈퇴한 요청자는 권한·적립 없이 반영만 한다.
        if (!editorRepository.canEdit(club.getId(), memberId)) {
            editorRepository.save(ClubEditor.grant(club.getId(), memberId, now));
        }
        if (firstRegistration || recruitedGeneration != null || updatedGeneration != null) {
            eventPublisher.publishEvent(new ClubRequestApprovedEvent(
                    memberId, club.getId(), firstRegistration, recruitedGeneration, updatedGeneration));
        }
    }

    @Transactional
    public void reject(Long requestId, String reason) {
        ClubRequest request = lockPending(requestId);
        try {
            request.reject(reason, clock.now());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_REJECT_REASON);
        }
    }

    /** 관리자 임시 등록. 회원 요청이 처음 승인되면 정식 등록으로 바뀌고 그 회원이 최초 등록 포인트를 받는다. */
    @Transactional
    public Long registerProvisional(ClubSaveCommand command) {
        ClubRequestPayload input = ClubInputs.infoPayload(command);
        photoBinder.requireUsableByAdmin(null, input.photos());
        Club club = clubRepository.save(Club.register(input.profile(), ClubRegistration.PROVISIONAL, clock.now()));
        photoBinder.bind(club.getId(), input.photos());
        return club.getId();
    }

    /** 관리자 정보 수정은 승인 없이 바로 반영하며 적립하지 않는다. */
    @Transactional
    public void edit(Long clubId, ClubSaveCommand command) {
        ClubRequestPayload input = ClubInputs.infoPayload(command);
        photoBinder.requireUsableByAdmin(clubId, input.photos());
        findClub(clubId).edit(input.profile(), clock.now());
        photoBinder.bind(clubId, input.photos());
        try {
            clubRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new GeneralException(ClubErrorCode.EDIT_CONFLICT);
        }
    }

    /** 회수하면 그 회원의 다음 정보 수정·모집 등록부터 다시 승인 요청이 된다. 없으면 아무것도 하지 않는다. */
    @Transactional
    public void revokeEditor(Long clubId, Long memberId) {
        findClub(clubId);
        editorRepository.deleteById(new ClubEditor.Key(clubId, memberId));
    }

    // 진행 중·예정 모집은 고쳐 쓰고, 없거나 마감됐으면 새 건을 만든다. 처음 모집하는 기수의 새 건이면 그 기수를 돌려준다.
    private Integer applyRecruitment(Long clubId, RecruitmentPlan plan, LocalDateTime now) {
        ClubRecruitment ongoing = recruitmentRepository
                .findTopByClubIdOrderByIdDesc(clubId)
                .filter(latest -> latest.schedule().statusAt(now) != RecruitmentStatus.CLOSED)
                .orElse(null);
        if (ongoing != null) {
            ongoing.revise(plan);
            return null;
        }
        Integer generation = plan.generation();
        boolean newGeneration =
                generation != null && !recruitmentRepository.existsByClubIdAndGeneration(clubId, generation);
        recruitmentRepository.save(ClubRecruitment.open(clubId, plan));
        return newGeneration ? generation : null;
    }

    private ClubRequest lockPending(Long requestId) {
        ClubRequest request = requestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new GeneralException(ClubErrorCode.REQUEST_NOT_FOUND));
        if (!request.isPending()) {
            throw new GeneralException(ClubErrorCode.REQUEST_ALREADY_PROCESSED);
        }
        return request;
    }

    private Club findClub(Long clubId) {
        return clubRepository.findById(clubId).orElseThrow(() -> new GeneralException(ClubErrorCode.CLUB_NOT_FOUND));
    }
}
