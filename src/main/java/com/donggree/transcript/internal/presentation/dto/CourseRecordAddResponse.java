package com.donggree.transcript.internal.presentation.dto;

import java.util.List;

/**
 * PATCH /api/users/me/reports 응답 DTO.
 * 새로 추가된 수강 이력의 ID 목록을 반환한다.
 */
public record CourseRecordAddResponse(List<Long> addedIds) {
}
