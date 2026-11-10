package com.donggree.graduation.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GraduationClassificationFallbackTest {
    @ParameterizedTest
    @CsvSource({"전필,FIRST_MAJOR,", "전선,FIRST_MAJOR,", "대기,LIBERAL_ARTS,대학전공기초", "나노,LIBERAL_ARTS,나노디그리"})
    void 원문이수구분의_리포트분류를_보존한다(String pdfType, String reportType, String area) {
        var classification = GraduationQueryService.inferClassification(pdfType);
        assertThat(classification.courseType().name()).isEqualTo(reportType);
        assertThat(classification.areaName()).isEqualTo(area);
    }
}
