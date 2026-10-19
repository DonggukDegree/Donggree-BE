package com.donggree.user.internal.infrastructure;

import com.donggree.user.internal.application.SchoolEmailMailDeliveryService;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** 공용 @Async 실행기와 분리된 메일 전용 워커와 메모리 대기열. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchoolEmailMailDispatcher {
    private static final Logger log = LoggerFactory.getLogger(SchoolEmailMailDispatcher.class);
    private final SchoolEmailMailDeliveryService deliveries;
    private final InMemorySchoolEmailMailQueue queue;
    private final boolean enabled;
    private final int workers;
    private final Semaphore available;
    private final ThreadPoolExecutor executor;
    private volatile boolean ready;

    public SchoolEmailMailDispatcher(
            SchoolEmailMailDeliveryService deliveries,
            InMemorySchoolEmailMailQueue queue,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.school-email.mail-queue.workers:3}") int workers) {
        if (workers < 1) throw new IllegalArgumentException("메일 워커 수는 1 이상이어야 합니다.");
        this.deliveries = deliveries;
        this.queue = queue;
        this.enabled = enabled;
        this.workers = workers;
        this.available = new Semaphore(workers);
        var sequence = new AtomicInteger();
        executor = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS, new SynchronousQueue<>(), runnable -> {
            var thread = new Thread(runnable, "school-email-mail-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        queue.open();
        ready = true;
    }

    @Scheduled(fixedDelayString = "${app.school-email.mail-queue.poll-interval-ms:500}")
    public void dispatch() {
        if (!ready || !enabled || available.availablePermits() == 0 || !deliveries.hasWork()) return;
        for (int i = 0; i < workers && available.tryAcquire(); i++) {
            try {
                executor.execute(() -> {
                    try {
                        // 대기 작업이 있으면 다음 폴링을 기다리지 않고 계속 처리한다.
                        while (!executor.isShutdown()
                                && !Thread.currentThread().isInterrupted()
                                && deliveries.deliverNext()) {
                            // SMTP 통신은 전용 워커에서만 수행한다.
                        }
                    } catch (RuntimeException exception) {
                        log.warn("학교 이메일 큐 처리에 실패했습니다.");
                    } finally {
                        available.release();
                    }
                });
            } catch (RejectedExecutionException exception) {
                available.release();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        ready = false;
        queue.close();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
