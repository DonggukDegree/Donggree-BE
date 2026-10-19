package com.donggree.user.internal.infrastructure;

import com.donggree.user.internal.application.SchoolEmailVerificationStore;
import com.donggree.user.internal.domain.SchoolEmailDeliveryStatus;
import com.donggree.user.internal.domain.SchoolEmailVerification;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 인증 코드 해시·시도 횟수·발송 상태를 메모리에만 저장한다. 재시작하면 모두 초기화된다. */
@Component
public class InMemorySchoolEmailVerificationStore implements SchoolEmailVerificationStore {
    private final ConcurrentHashMap<Long, SchoolEmailVerification> verifications = new ConcurrentHashMap<>();
    private final ReentrantLock[] locks = new ReentrantLock[256];

    public InMemorySchoolEmailVerificationStore() {
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
    }

    @Override
    public Optional<SchoolEmailVerification> findById(Long memberId) {
        return Optional.ofNullable(verifications.get(memberId)).map(SchoolEmailVerification::copy);
    }

    @Override
    public Optional<SchoolEmailVerification> findByIdForUpdate(Long memberId) {
        lockUntilTransactionCompletes(memberId);
        return findById(memberId);
    }

    @Override
    public void save(SchoolEmailVerification verification) {
        SchoolEmailVerification snapshot = verification.copy();
        afterCommit(() -> verifications.put(snapshot.getMemberId(), snapshot));
    }

    @Override
    public void deleteById(Long memberId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) lockUntilTransactionCompletes(memberId);
        afterCommit(() -> verifications.remove(memberId));
    }

    @Override
    public boolean startDelivery(Long memberId, UUID requestId) {
        var started = new AtomicBoolean();
        verifications.computeIfPresent(memberId, (id, current) -> {
            if (!current.isQueued(requestId)) return current;
            SchoolEmailVerification next = current.copy();
            next.startDelivery();
            started.set(true);
            return next;
        });
        return started.get();
    }

    @Override
    public void completeDelivery(Long memberId, UUID requestId, boolean succeeded, LocalDateTime now) {
        verifications.computeIfPresent(memberId, (id, current) -> {
            if (!current.isSending(requestId)) return current;
            SchoolEmailVerification next = current.copy();
            if (succeeded) next.finishDelivery(now);
            else next.failDelivery();
            return next;
        });
    }

    @Scheduled(fixedDelay = 60_000)
    public void removeExpiredEntries() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        // 당일 발송 제한은 유지한다. 대기·발송 중인 작업은 경과 시간으로 지우지 않는다.
        for (Long memberId : verifications.keySet()) {
            verifications.computeIfPresent(memberId, (id, current) -> {
                boolean delivering = current.getDeliveryStatus() == SchoolEmailDeliveryStatus.QUEUED
                        || current.getDeliveryStatus() == SchoolEmailDeliveryStatus.SENDING;
                return !delivering
                                && current.getSendDate().isBefore(now.toLocalDate())
                                && current.isExpired(now)
                                && current.canResend(now)
                        ? null
                        : current;
            });
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public int getOrder() {
                // 인증 상태를 먼저 반영한 뒤 대기 작업을 워커에 공개한다.
                return Integer.MAX_VALUE - 2;
            }

            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void lockUntilTransactionCompletes(Long memberId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("인증 상태 변경에는 트랜잭션이 필요합니다.");
        // DB 커밋 직후 메모리 반영이 끝나기 전에 다음 요청이 이전 상태를 읽는 틈을 막는다.
        ReentrantLock lock = locks[Math.floorMod(memberId.hashCode(), locks.length)];
        lock.lock();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
    }
}
