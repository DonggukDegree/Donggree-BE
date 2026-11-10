package com.donggree.graduation.internal.domain.rule;

import java.util.List;

/** 원 필수과목을 이수하지 않았을 때 대체과목으로 감면 가능한 학점. */
public record CreditAdjustment(List<String> requiredCourseCodes, List<String> replacementCourseCodes, int credits) {
    public CreditAdjustment {
        requiredCourseCodes = List.copyOf(requiredCourseCodes);
        replacementCourseCodes = List.copyOf(replacementCourseCodes);
    }
}
