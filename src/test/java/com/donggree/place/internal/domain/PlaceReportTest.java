package com.donggree.place.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.donggree.place.internal.domain.enums.PlaceReportReason;
import com.donggree.place.internal.domain.enums.PlaceReportStatus;
import org.junit.jupiter.api.Test;

class PlaceReportTest {

    @Test
    void 신고는_처리_대기로_접수하고_공백_설명은_미입력으로_본다() {
        PlaceReport report = PlaceReport.submit(1L, 7L, PlaceReportReason.CLOSED, "  ");

        assertThat(report.getStatus()).isEqualTo(PlaceReportStatus.PENDING);
        assertThat(report.isPending()).isTrue();
        assertThat(report.getDescription()).isNull();
        assertThat(report.getProcessedAt()).isNull();
        assertThat(PlaceReport.submit(1L, 7L, PlaceReportReason.WRONG_NAME, " 간판은 동국각 ")
                        .getDescription())
                .isEqualTo("간판은 동국각");
    }

    @Test
    void 기타_사유는_설명이_필요하다() {
        assertThatThrownBy(() -> PlaceReport.submit(1L, 7L, PlaceReportReason.OTHER, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PlaceReport.submit(1L, 7L, PlaceReportReason.OTHER, "주차장 입구예요")
                        .getReason())
                .isEqualTo(PlaceReportReason.OTHER);
    }

    @Test
    void 설명은_200자까지_허용하고_필수값이_없으면_만들_수_없다() {
        assertThat(PlaceReport.submit(1L, 7L, PlaceReportReason.CLOSED, "가".repeat(200))
                        .getDescription())
                .hasSize(200);
        assertThatThrownBy(() -> PlaceReport.submit(1L, 7L, PlaceReportReason.CLOSED, "가".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReport.submit(1L, 7L, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReport.submit(1L, null, PlaceReportReason.CLOSED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlaceReport.submit(null, 7L, PlaceReportReason.CLOSED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
