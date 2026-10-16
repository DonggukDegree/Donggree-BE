package com.donggree.graduation.internal.presentation.dto;

import com.donggree.curriculum.CourseType;
import com.donggree.graduation.internal.application.readmodel.ReportPreviewReadModel;
import java.util.LinkedHashMap;
import java.util.Map;

/** 사용자 리포트와 같은 HTTP 요약·상세 계약. 이름·학번·PDF 원문은 포함하지 않는다. */
public record AdminReportPreviewResponse(GraduationReportResponse report, Map<CourseType, AreaDetailResponse> details) {
    public static AdminReportPreviewResponse from(ReportPreviewReadModel source) {
        Map<CourseType, AreaDetailResponse> details = new LinkedHashMap<>();
        source.details().forEach((type, detail) -> details.put(type, AreaDetailResponse.from(detail)));
        return new AdminReportPreviewResponse(GraduationReportResponse.from(source.report()), details);
    }
}
