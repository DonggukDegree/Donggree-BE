package com.donggree.graduation.internal.presentation.dto;

import com.donggree.graduation.internal.application.readmodel.GraduationReportReadModel;
import java.math.BigDecimal;
import java.util.List;

/** 학업 리포트 HTTP 계약. 응용 계층의 읽기 모델을 중첩 항목까지 명시적으로 변환한다. */
public record GraduationReportResponse(
        Summary summary,
        List<AreaOverview> areaOverviews,
        boolean hasUnsupportedMajor,
        Boolean englishPassed,
        List<AdditionalNotice> additionalNotices) {
    public static GraduationReportResponse from(GraduationReportReadModel source) {
        return new GraduationReportResponse(
                Summary.from(source.summary()),
                source.areaOverviews().stream().map(AreaOverview::from).toList(),
                source.hasUnsupportedMajor(),
                source.englishPassed(),
                source.additionalNotices().stream().map(AdditionalNotice::from).toList());
    }

    public record Summary(
            int achievementRate,
            int earnedCredits,
            int targetCredits,
            int remainingCredits,
            BigDecimal gpa,
            boolean graduated,
            List<String> unsatisfiedReasons) {
        static Summary from(GraduationReportReadModel.Summary source) {
            return new Summary(
                    source.achievementRate(),
                    source.earnedCredits(),
                    source.targetCredits(),
                    source.remainingCredits(),
                    source.gpa(),
                    source.graduated(),
                    source.unsatisfiedReasons());
        }
    }

    public record AreaOverview(
            String courseType, String courseTypeName, int achievementRate, int remainingCredits, boolean satisfied) {
        static AreaOverview from(GraduationReportReadModel.AreaOverview source) {
            return new AreaOverview(
                    source.courseType(),
                    source.courseTypeName(),
                    source.achievementRate(),
                    source.remainingCredits(),
                    source.satisfied());
        }
    }

    public record AdditionalNotice(
            Long requirementSetId,
            String collegeName,
            String departmentName,
            String track,
            int yearStart,
            int yearEnd,
            String content) {
        static AdditionalNotice from(GraduationReportReadModel.AdditionalNotice source) {
            return new AdditionalNotice(
                    source.requirementSetId(),
                    source.collegeName(),
                    source.departmentName(),
                    source.track(),
                    source.yearStart(),
                    source.yearEnd(),
                    source.content());
        }
    }
}
