package com.donggree.graduation.internal.presentation.dto;

import com.donggree.curriculum.CourseType;
import com.donggree.graduation.internal.application.projection.AreaDetailProjection;
import com.donggree.graduation.internal.application.projection.GraduationReportProjection;
import com.donggree.graduation.internal.application.projection.ReportPreviewProjection;
import java.util.Map;

/** 사용자 리포트와 같은 요약·상세 구조만 반환하며 이름·학번·PDF 원문은 포함하지 않는다. */
public record AdminReportPreviewResponse(
        GraduationReportProjection report, Map<CourseType, AreaDetailProjection> details) {
    public static AdminReportPreviewResponse from(ReportPreviewProjection preview) {
        return new AdminReportPreviewResponse(preview.report(), preview.details());
    }
}
