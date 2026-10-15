package com.donggree.global.metrics;

/** 업무 모듈이 기록하는 관측 속성 계약. 요청 속성 키는 기존 필터와 호환한다. */
@org.springframework.modulith.NamedInterface("metrics")
public final class OperationMetricsContext {
    public static final String CODE = "com.donggree.global.metrics.OperationMetricsFilter.code";
    public static final String OUTCOME = "com.donggree.global.metrics.OperationMetricsFilter.outcome";
    public static final String ACTOR = "com.donggree.global.metrics.OperationMetricsFilter.actor";

    private OperationMetricsContext() {}
}
