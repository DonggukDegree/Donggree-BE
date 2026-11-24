package com.donggree.mystery.event;

import java.time.LocalDate;

/** 회원이 해당 날짜의 정답을 처음 해결했다. 정답 저장과 보상은 같은 트랜잭션에서 완료한다. */
public record MysterySolvedEvent(Long memberId, LocalDate gameDate) {}
