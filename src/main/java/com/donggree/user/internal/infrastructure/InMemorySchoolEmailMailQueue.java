package com.donggree.user.internal.infrastructure;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.internal.application.SchoolEmailMailQueue;
import com.donggree.user.internal.application.command.SchoolEmailMailDelivery;
import com.donggree.user.internal.application.exception.SchoolEmailErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 회원마다 최신 작업 하나만 보관하는 메일 전용 메모리 큐. 대기 시간으로 작업을 폐기하지 않는다. */
@Component
public class InMemorySchoolEmailMailQueue implements SchoolEmailMailQueue {
    private final Map<Long, Pending> pending = new LinkedHashMap<>();
    private final int capacity;
    private boolean accepting;

    public InMemorySchoolEmailMailQueue(@Value("${app.school-email.mail-queue.capacity:36}") int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("메일 대기열 크기는 1 이상이어야 합니다.");
        this.capacity = capacity;
    }

    @Override
    public synchronized void enqueue(SchoolEmailMailDelivery delivery) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("메일 접수에는 트랜잭션이 필요합니다.");
        if (!accepting) throw new GeneralException(SchoolEmailErrorCode.SEND_UNAVAILABLE);
        Long memberId = delivery.memberId();
        Pending previous = pending.get(memberId);
        // 같은 회원의 작업 교체는 대기열이 가득 차 있어도 추가 자리를 차지하지 않는다.
        if (previous == null && pending.size() >= capacity) throw new GeneralException(SchoolEmailErrorCode.QUEUE_FULL);
        Pending replacement = new Pending(delivery);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public int getOrder() {
                // 롤백 시 인증 상태의 회원 잠금을 풀기 전에 기존 대기 작업을 복원한다.
                return Integer.MAX_VALUE - 1;
            }

            @Override
            public void afterCommit() {
                synchronized (InMemorySchoolEmailMailQueue.this) {
                    replacement.ready = true;
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) return;
                synchronized (InMemorySchoolEmailMailQueue.this) {
                    if (pending.get(memberId) != replacement) return;
                    pending.remove(memberId);
                    if (previous != null) pending.put(memberId, previous);
                }
            }
        });
        // 최신 재발송은 뒤로 보내 다른 회원의 대기 순서를 보존한다.
        pending.remove(memberId);
        pending.put(memberId, replacement);
    }

    @Override
    public synchronized boolean hasWork() {
        return pending.values().stream().anyMatch(job -> job.ready);
    }

    @Override
    public synchronized Optional<SchoolEmailMailDelivery> poll() {
        var iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            Pending job = iterator.next();
            if (!job.ready) continue;
            iterator.remove();
            return Optional.of(job.delivery);
        }
        return Optional.empty();
    }

    @Override
    public void cancel(Long memberId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            remove(memberId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                remove(memberId);
            }
        });
    }

    private synchronized void remove(Long memberId) {
        pending.remove(memberId);
    }

    public synchronized void open() {
        accepting = true;
    }

    public synchronized void close() {
        accepting = false;
    }

    private static final class Pending {
        private final SchoolEmailMailDelivery delivery;
        private boolean ready;

        private Pending(SchoolEmailMailDelivery delivery) {
            this.delivery = delivery;
        }
    }
}
