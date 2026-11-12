package com.donggree.user;

import java.time.LocalDateTime;

/** 정지 이력 한 건. 해제했으면 liftedAt이 있다. */
public record MemberSuspensionRecord(
        Long id,
        int days,
        SuspensionSource source,
        ReportTargetType reportType,
        Long reportId,
        String note,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long createdBy,
        LocalDateTime liftedAt,
        Long liftedBy) {}
