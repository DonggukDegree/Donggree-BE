package com.donggree.graduation.internal.application.report;

import java.util.List;
import java.util.Optional;

/** 표시용 설정의 기존 기본값·관대한 해석을 제공한다. 판정용 설정 검증과 구분한다. */
public interface ReportSettingsReader {
    List<String> requiredCodes(String settings);

    List<String> stringList(String settings, String field);

    boolean hasNumber(String settings, String field);

    int intValue(String settings, String field, int defaultValue);

    Optional<CreditTarget> creditTarget(String settings, List<String> selectorFields);

    record CreditTarget(int credits, List<List<String>> selectors) {}
}
