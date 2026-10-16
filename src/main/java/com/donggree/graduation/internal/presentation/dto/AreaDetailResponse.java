package com.donggree.graduation.internal.presentation.dto;

import com.donggree.graduation.internal.application.readmodel.AreaDetailReadModel;
import java.util.List;

/** 영역별 이수 현황의 HTTP 계약. */
public record AreaDetailResponse(
        List<AreaSection> areaDetails, List<String> unsatisfiedReasons, CreditStatus creditStatus) {
    public static AreaDetailResponse from(AreaDetailReadModel source) {
        return new AreaDetailResponse(
                source.areaDetails().stream().map(AreaSection::from).toList(),
                source.unsatisfiedReasons(),
                CreditStatus.from(source.creditStatus()));
    }

    public record AreaSection(
            String areaName, int earnedCredits, int targetCredits, boolean satisfied, List<CourseItem> items) {
        static AreaSection from(AreaDetailReadModel.AreaSection source) {
            return new AreaSection(
                    source.areaName(),
                    source.earnedCredits(),
                    source.targetCredits(),
                    source.satisfied(),
                    source.items().stream().map(CourseItem::from).toList());
        }
    }

    public record CourseItem(String title, int credit, String status, List<String> detail) {
        static CourseItem from(AreaDetailReadModel.CourseItem source) {
            return new CourseItem(source.title(), source.credit(), source.status(), source.detail());
        }
    }

    public record CreditStatus(int earnedCredits, int targetCredits, int remainingCredits) {
        static CreditStatus from(AreaDetailReadModel.CreditStatus source) {
            return new CreditStatus(source.earnedCredits(), source.targetCredits(), source.remainingCredits());
        }
    }
}
