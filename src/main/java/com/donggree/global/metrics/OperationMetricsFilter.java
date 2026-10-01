package com.donggree.global.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 사용자 수가 아닌 서버 처리 횟수·시간. 보안 필터와 트랜잭션 완료까지 포함한다. */
@Component
@Order(-101)
@RequiredArgsConstructor
public class OperationMetricsFilter extends OncePerRequestFilter {
    public static final String CODE = OperationMetricsFilter.class.getName() + ".code";
    public static final String OUTCOME = OperationMetricsFilter.class.getName() + ".outcome";
    public static final String ACTOR = OperationMetricsFilter.class.getName() + ".actor";
    private final MeterRegistry registry;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return operation(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            failed = true;
            throw ex;
        } finally {
            // 모니터링 장애가 로그인·성적표 저장 응답을 바꾸면 안 된다.
            try {
                String code = failed ? "UNHANDLED_ERROR" : code(request, response);
                String outcome =
                        failed || response.getStatus() >= 400 || "failure".equals(request.getAttribute(OUTCOME))
                                ? "failure"
                                : "success";
                String actor = request.getAttribute(ACTOR) instanceof String value
                                && (value.equals("student") || value.equals("admin"))
                        ? value
                        : "unknown";
                Timer.builder("donggree.operation")
                        .description("서버 업무 처리 횟수와 시간; 실제 사용자 수가 아님")
                        .tags(
                                "operation",
                                operation(request),
                                "outcome",
                                outcome,
                                "code",
                                code,
                                "http_status",
                                String.valueOf(response.getStatus()),
                                "actor",
                                actor)
                        .register(registry)
                        .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            } catch (RuntimeException ignored) {
                // 외부 측정 시스템 실패는 업무 실패로 취급하지 않는다.
            }
        }
    }

    private static String code(HttpServletRequest request, HttpServletResponse response) {
        Object code = request.getAttribute(CODE);
        if (code instanceof String value
                && value.matches(
                        "(?:COMMON|USER|TRANSCRIPT|GRADUATION)[0-9]{3}_[0-9]{1,2}|LOGIN_SUCCESS|LOGIN_FAILURE"))
            return value;
        return response.getStatus() >= 400 ? "HTTP_ERROR" : "OK";
    }

    static String operation(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("GET".equals(request.getMethod()) && "/oauth/callback/kakao".equals(path)) return "login";
        if ("PUT".equals(request.getMethod()) && "/api/users/me/reports".equals(path)) return "pdf_upload";
        if ("GET".equals(request.getMethod()) && "/api/reports/summary".equals(path)) return "report_summary";
        return null;
    }
}
