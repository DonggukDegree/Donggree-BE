package com.donggree.user.internal.application;

import com.donggree.user.internal.application.command.SchoolEmailMailDelivery;
import java.util.Optional;

/** 학교 이메일 전용 대기열. 접수 트랜잭션이 커밋된 작업만 발송에 넘긴다. */
public interface SchoolEmailMailQueue {
    void enqueue(SchoolEmailMailDelivery delivery);

    boolean hasWork();

    Optional<SchoolEmailMailDelivery> poll();

    void cancel(Long memberId);
}
