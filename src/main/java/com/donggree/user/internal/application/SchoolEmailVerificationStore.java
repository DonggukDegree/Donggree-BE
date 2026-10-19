package com.donggree.user.internal.application;

import com.donggree.user.internal.domain.SchoolEmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** 미완료 이메일 인증의 임시 저장소. 조회 결과는 저장된 상태와 분리된 복사본이다. */
public interface SchoolEmailVerificationStore {
    Optional<SchoolEmailVerification> findById(Long memberId);

    Optional<SchoolEmailVerification> findByIdForUpdate(Long memberId);

    void save(SchoolEmailVerification verification);

    void deleteById(Long memberId);

    boolean startDelivery(Long memberId, UUID requestId);

    void completeDelivery(Long memberId, UUID requestId, boolean succeeded, LocalDateTime now);
}
