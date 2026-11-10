package com.donggree.global.config;

import com.donggree.global.logging.RequestLoggingFilter;
import com.donggree.global.metrics.OperationMetricsFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 필터는 일반 객체로 생성하고 등록 정보만 Spring 빈으로 관리한다.
 * Modulith 관측 프록시가 GenericFilterBean의 final init()과 충돌하는 것을 방지한다.
 */
@Configuration(proxyBeanMethods = false)
public class ServletFilterConfig {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        var registration = new FilterRegistrationBean<>(new RequestLoggingFilter());
        registration.setOrder(-102);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<OperationMetricsFilter> operationMetricsFilter(MeterRegistry meterRegistry) {
        var registration = new FilterRegistrationBean<>(new OperationMetricsFilter(meterRegistry));
        registration.setOrder(-101);
        return registration;
    }
}
