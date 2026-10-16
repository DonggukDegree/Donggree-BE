package com.donggree.graduation.internal.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.donggree.curriculum.CourseType;
import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import com.donggree.graduation.internal.application.readmodel.GraduationReportReadModel;
import com.donggree.graduation.internal.application.readmodel.ReportPreviewReadModel;
import com.donggree.graduation.internal.presentation.dto.AdminReportPreviewResponse;
import com.donggree.graduation.internal.presentation.dto.AreaDetailResponse;
import com.donggree.graduation.internal.presentation.dto.GraduationReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportResponseContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 응답_분리_후에도_요약과_영역_상세의_기존_JSON_계약을_보존한다() {
        var summary = summary();
        var detail = detail();
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(GraduationReportResponse.from(summary)))
                .isEqualTo(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(summary));
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(AreaDetailResponse.from(detail)))
                .isEqualTo(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(detail));
    }

    @Test
    void 미리보기도_중첩_응답을_변환하며_null과_빈_목록을_보존한다() {
        var preview = new ReportPreviewReadModel(summary(), Map.of(CourseType.FIRST_MAJOR, detail()));
        var response = AdminReportPreviewResponse.from(preview);
        assertThat(response.report()).isInstanceOf(GraduationReportResponse.class);
        assertThat(response.details().get(CourseType.FIRST_MAJOR)).isInstanceOf(AreaDetailResponse.class);
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(response))
                .isEqualTo(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(preview));
    }

    private GraduationReportReadModel summary() {
        return new GraduationReportReadModel(
                new GraduationReportReadModel.Summary(
                        70, 91, 130, 39, new BigDecimal("3.50"), false, List.of("미충족 기준")),
                List.of(new GraduationReportReadModel.AreaOverview("FIRST_MAJOR", "제1전공", 50, 12, false)),
                true,
                null,
                List.of(new GraduationReportReadModel.AdditionalNotice(10L, null, "학과", null, 2021, 2025, "안내")));
    }

    private AreaDetailReadModel detail() {
        return new AreaDetailReadModel(
                List.of(new AreaDetailReadModel.AreaSection(
                        "전공기초",
                        3,
                        6,
                        false,
                        List.of(
                                new AreaDetailReadModel.CourseItem("과목", 3, "OPTIONAL", null),
                                new AreaDetailReadModel.CourseItem("실험", 0, "UNSATISFIED", List.of())))),
                List.of("미충족 기준"),
                new AreaDetailReadModel.CreditStatus(3, 6, 3));
    }
}
