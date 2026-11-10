package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.command.RequestSchoolEmailCodeCommand;
import com.donggree.user.internal.application.command.SchoolEmailCodeRequestResult;
import com.donggree.user.internal.application.command.SchoolEmailMailDelivery;
import com.donggree.user.internal.application.command.VerifySchoolEmailCodeCommand;
import com.donggree.user.internal.application.exception.SchoolEmailCodeMismatchException;
import com.donggree.user.internal.application.exception.SchoolEmailErrorCode;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import com.donggree.user.internal.domain.SchoolEmailAddress;
import com.donggree.user.internal.domain.SchoolEmailDeliveryStatus;
import com.donggree.user.internal.domain.SchoolEmailVerification;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolEmailVerificationCommandService {
    private final MemberRepository members;
    private final SchoolEmailVerificationStore verifications;
    private final SchoolEmailMailQueue mailQueue;
    private final SchoolEmailCodes codes;
    private final SchoolEmailTelemetry telemetry;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Transactional
    public SchoolEmailCodeRequestResult requestCode(Long memberId, RequestSchoolEmailCodeCommand command) {
        telemetry.identify(memberId, null);
        SchoolEmailAddress address = address(command.localPart());
        telemetry.identify(memberId, address.value());
        if (!mailEnabled) throw new GeneralException(SchoolEmailErrorCode.SEND_UNAVAILABLE);
        // 회원 행 잠금으로 첫 요청·재발송·코드 확인을 같은 순서로 처리한다.
        Member member = unverifiedMember(memberId);
        ensureAvailable(address.value(), memberId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        SchoolEmailVerification verification =
                verifications.findByIdForUpdate(memberId).orElseGet(() -> new SchoolEmailVerification(memberId));
        if (!verification.canResend(now)) {
            throw new GeneralException(SchoolEmailErrorCode.RESEND_LIMIT);
        }
        if (verification.reachedDailyLimit(now)) {
            throw new GeneralException(SchoolEmailErrorCode.DAILY_LIMIT);
        }
        String code = codes.generate();
        UUID requestId = UUID.randomUUID();
        verification.enqueue(address, codes.hash(member.getId(), address.value(), code), requestId, now);
        verifications.save(verification);
        // 용량 확보 실패 시 DB 변경도 롤백하며, 커밋 전에는 워커가 가져가지 못한다.
        mailQueue.enqueue(new SchoolEmailMailDelivery(memberId, requestId, address.value(), code));
        return new SchoolEmailCodeRequestResult(
                address.value(),
                0,
                SchoolEmailVerification.RESEND_SECONDS,
                requestId,
                verification.getDeliveryStatus().name());
    }

    @Transactional(noRollbackFor = SchoolEmailCodeMismatchException.class)
    public void verifyCode(Long memberId, VerifySchoolEmailCodeCommand command) {
        telemetry.identify(memberId, null);
        SchoolEmailAddress address = address(command.localPart());
        telemetry.identify(memberId, address.value());
        Member member = unverifiedMember(memberId);
        SchoolEmailVerification verification = verifications
                .findByIdForUpdate(memberId)
                .orElseThrow(() -> new GeneralException(SchoolEmailErrorCode.NOT_REQUESTED));
        if (verification.getDeliveryStatus() == SchoolEmailDeliveryStatus.FAILED)
            throw new GeneralException(SchoolEmailErrorCode.SEND_UNAVAILABLE);
        if (verification.getDeliveryStatus() != SchoolEmailDeliveryStatus.SENT)
            throw new GeneralException(SchoolEmailErrorCode.DELIVERY_PENDING);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (verification.isExpired(now)) {
            throw new GeneralException(SchoolEmailErrorCode.EXPIRED);
        }
        if (verification.isLocked()) {
            throw new GeneralException(SchoolEmailErrorCode.ATTEMPT_LIMIT);
        }
        if (!address.value().equals(verification.getEmail())
                || !codes.matches(memberId, address.value(), command.code(), verification.getCodeHash())) {
            verification.recordFailure();
            verifications.save(verification);
            throw new SchoolEmailCodeMismatchException(verification.isLocked());
        }
        ensureAvailable(address.value(), memberId);
        member.verifySchoolEmail(address, now);
        try {
            members.flush();
        } catch (DataIntegrityViolationException exception) {
            // 서로 다른 회원의 동시 인증은 DB의 이메일 유니크 제약으로 최종 차단한다.
            throw new GeneralException(SchoolEmailErrorCode.EMAIL_IN_USE);
        }
        verifications.deleteById(memberId);
        mailQueue.cancel(memberId);
    }

    private Member unverifiedMember(Long memberId) {
        Member member = members.findByIdForUpdate(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));
        if (!member.hasCompletedOnboarding()) {
            throw new GeneralException(UserErrorCode.NOT_ONBOARDED);
        }
        // PDF 본인인증과 학교 이메일 인증은 별도 상태이므로 학교 이메일 완료 여부만 막는다.
        if (member.isSchoolEmailVerified()) {
            throw new GeneralException(SchoolEmailErrorCode.ALREADY_VERIFIED);
        }
        return member;
    }

    private void ensureAvailable(String email, Long memberId) {
        if (members.existsBySchoolEmailAndIdNot(email, memberId)) {
            throw new GeneralException(SchoolEmailErrorCode.EMAIL_IN_USE);
        }
    }

    private SchoolEmailAddress address(String localPart) {
        try {
            return SchoolEmailAddress.fromLocalPart(localPart);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(SchoolEmailErrorCode.INVALID_EMAIL);
        }
    }
}
