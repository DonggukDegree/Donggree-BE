package com.donggree.club.internal.application.readmodel;

import java.util.List;

/**
 * 목록 한 묶음.
 *
 * @param nextCursor 다음 묶음 커서. 마지막이면 null
 * @param totalCount 필터를 적용한 전체 개수
 */
public record ClubPageReadModel(List<ClubSummaryReadModel> clubs, String nextCursor, int totalCount) {}
