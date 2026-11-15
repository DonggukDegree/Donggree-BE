package com.donggree.point.internal.presentation.dto;

import com.donggree.point.internal.domain.PointReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 방금 한 활동의 적립 안내 확인 요청. */
public record PointRewardAcknowledgeRequest(
        @NotNull(message = "적립 사유는 필수입니다.") @Schema(example = "PLACE_INFO_EDIT") PointReason reason,
        @Schema(example = "10", description = "장소 활동이면 장소 ID, 졸업 리포트 최초 확인이면 생략") Long targetId) {}
