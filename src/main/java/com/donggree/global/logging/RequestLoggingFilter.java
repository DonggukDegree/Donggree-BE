package com.donggree.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 보안 필터 밖에서 요청 ID를 발급하고, 응답 완료 시 실패를 한 번만 기록한다. 관리자 오류도 포함한다. */
@Component
@Order(-102)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = RequestLoggingFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, requestId);
        response.setHeader("X-Request-ID", requestId);
        MDC.put("request_id", requestId);
        String method = request.getMethod();
        MDC.put(
                "http_method",
                Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
                                .contains(method)
                        ? method
                        : "OTHER");
        long start = System.nanoTime();
        boolean escaped = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            escaped = true;
            RequestDiagnostics.failure(request, "UNHANDLED_ERROR", "request", "요청 처리 중 미처리 예외", ex);
            throw ex;
        } finally {
            try {
                Object detail = request.getAttribute(RequestDiagnostics.FAILURE);
                int status = response.getStatus();
                if ((detail instanceof RequestDiagnostics.Failure || status >= 400)
                        && request.getAttribute(RequestDiagnostics.LOGGED) == null) {
                    request.setAttribute(RequestDiagnostics.LOGGED, true);
                    RequestDiagnostics.Failure failure = detail instanceof RequestDiagnostics.Failure known
                            ? known
                            : new RequestDiagnostics.Failure("HTTP_" + status, "response", "오류 HTTP 응답", null);
                    RequestDiagnostics.log(
                            request,
                            status,
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                            failure,
                            escaped,
                            response.isCommitted());
                }
            } catch (RuntimeException ignored) {
                // 로그 출력 실패가 서비스 응답·원본 예외를 덮지 않도록 한다.
            } finally {
                if (previous == null) MDC.clear();
                else MDC.setContextMap(previous);
            }
        }
    }
}
