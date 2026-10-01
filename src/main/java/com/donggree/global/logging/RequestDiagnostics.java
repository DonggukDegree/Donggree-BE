package com.donggree.global.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/** 요청 원인 추적용 로그. 외부 입력·예외 메시지·본문 대신 코드, 예외 종류, 코드 위치만 기록한다. */
public final class RequestDiagnostics {
    static final String FAILURE = RequestDiagnostics.class.getName() + ".failure";
    static final String LOGGED = RequestDiagnostics.class.getName() + ".logged";
    public static final String AUTH_REASON = RequestDiagnostics.class.getName() + ".authReason";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RequestDiagnostics.class);

    private RequestDiagnostics() {}

    public record Failure(String code, String stage, String reason, Throwable cause) {}

    /** reason은 서버가 정의한 설명만 전달한다. 예외 메시지나 사용자 입력을 전달하지 않는다. */
    public static void failure(HttpServletRequest request, String code, String stage, String reason, Throwable cause) {
        request.setAttribute(FAILURE, new Failure(code, stage, reason, cause));
    }

    public static void authenticationFailure(Throwable cause) {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            request.setAttribute(
                    AUTH_REASON,
                    cause instanceof io.jsonwebtoken.ExpiredJwtException ? "TOKEN_EXPIRED" : "TOKEN_INVALID");
        }
    }

    /** 정상 응답으로 대체한 데이터 해석 실패도 남긴다. 같은 요청·단계의 반복은 한 번으로 제한한다. */
    public static void fallback(String stage, Throwable cause) {
        HttpServletRequest request = currentRequest();
        String key = RequestDiagnostics.class.getName() + ".fallback." + stage;
        if (request != null) {
            if (request.getAttribute(key) != null) return;
            request.setAttribute(key, true);
        }
        try {
            LoggingEventBuilder event = LOG.atWarn()
                    .addKeyValue("event", "data_fallback")
                    .addKeyValue("code", "DATA_FALLBACK")
                    .addKeyValue("stage", stage)
                    .addKeyValue("reason", "데이터 해석 실패로 기본값 사용")
                    .addKeyValue("endpoint", endpoint(request));
            exceptionFields(event, cause);
            event.log("데이터 해석 실패: 기본값 대체");
        } catch (RuntimeException ignored) {
            // 로깅 장애로 기존 응답 처리를 바꾸지 않는다.
        }
    }

    static void log(
            HttpServletRequest request,
            int status,
            long durationMs,
            Failure failure,
            boolean escaped,
            boolean committed) {
        LoggingEventBuilder event = (status >= 500 || escaped ? LOG.atError() : LOG.atWarn())
                .addKeyValue("event", "request_failure")
                .addKeyValue("endpoint", endpoint(request))
                .addKeyValue("http_status", status)
                .addKeyValue("duration_ms", durationMs)
                .addKeyValue("exception_escaped", escaped)
                .addKeyValue("response_committed", committed)
                .addKeyValue("code", failure.code())
                .addKeyValue("stage", failure.stage())
                .addKeyValue("reason", failure.reason());
        Object auth = request.getAttribute(AUTH_REASON);
        if (auth != null) event.addKeyValue("auth_reason", auth);
        exceptionFields(event, failure.cause());
        event.log("요청 처리 실패");
    }

    /** 매핑된 경로의 {id}는 유지하고 쿼리·임의 URL·실제 식별자 값은 기록하지 않는다. */
    static String endpoint(HttpServletRequest request) {
        if (request == null) return "BACKGROUND";
        Object template = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (template != null && !template.toString().equals("/**")) return template.toString();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (Set.of("/oauth/callback/kakao", "/oauth2/authorization/kakao", "/auth/refresh", "/auth/logout")
                .contains(path)) return path;
        if (path.equals("/api/admin") || path.startsWith("/api/admin/")) return "/api/admin/**";
        if (path.equals("/actuator") || path.startsWith("/actuator/")) return "/actuator/**";
        if (path.equals("/api") || path.startsWith("/api/")) return "/api/**";
        return "UNMATCHED";
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest()
                : null;
    }

    private static void exceptionFields(LoggingEventBuilder event, Throwable cause) {
        if (cause == null) return;
        event.addKeyValue("exception_type", cause.getClass().getName());
        StringBuilder stack = new StringBuilder();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = cause;
        Throwable root = cause;
        String sqlState = null;
        int sqlError = 0;
        boolean framesTruncated = false;
        // 예외 메시지에는 SQL 값·PDF 원문·토큰이 들어갈 수 있어 절대 직렬화하지 않는다.
        for (int depth = 0; current != null && depth < 16 && visited.add(current); depth++) {
            root = current;
            if (depth > 0) stack.append("Caused by: ");
            stack.append(current.getClass().getName()).append('\n');
            StackTraceElement[] frames = current.getStackTrace();
            for (int i = 0; i < Math.min(frames.length, 80); i++)
                stack.append("  at ").append(frames[i]).append('\n');
            if (frames.length > 80) {
                framesTruncated = true;
                stack.append("  ... 추가 프레임 생략\n");
            }
            if (current instanceof SQLException sql
                    && sql.getSQLState() != null
                    && sql.getSQLState().matches("[A-Z0-9]{5}")) {
                sqlState = sql.getSQLState();
                sqlError = sql.getErrorCode();
            }
            if (current instanceof com.fasterxml.jackson.core.JsonProcessingException json
                    && json.getLocation() != null) {
                event.addKeyValue("json_line", json.getLocation().getLineNr());
                event.addKeyValue("json_column", json.getLocation().getColumnNr());
            }
            if (current instanceof org.springframework.web.bind.MethodArgumentNotValidException validation) {
                event.addKeyValue(
                        "validation_fields",
                        validation.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField().replaceAll("\\[[^\\]]*\\]", "[]"))
                                .filter(field -> field.length() <= 100 && field.matches("[A-Za-z0-9_.\\[\\]]+"))
                                .distinct()
                                .limit(20)
                                .toList());
            }
            current = current.getCause();
        }
        event.addKeyValue("root_exception_type", root.getClass().getName());
        if (sqlState != null) {
            event.addKeyValue("sql_state", sqlState);
            event.addKeyValue("sql_error_code", sqlError);
            event.addKeyValue(
                    "cause_summary",
                    switch (sqlState) {
                        case "23505" -> "고유값 중복으로 DB 저장 거절";
                        case "23503" -> "참조 대상 부재 또는 참조 중인 데이터 변경";
                        case "23502" -> "DB 필수값 누락";
                        case "23514" -> "DB 값 검증 조건 위반";
                        case "40001" -> "동시 변경으로 트랜잭션 충돌";
                        case "40P01" -> "DB 트랜잭션 교착 상태";
                        default -> sqlState.startsWith("08") ? "DB 연결 오류" : "SQL 상태 코드 확인 필요";
                    });
        }
        // Loki의 행 크기 제한을 넘지 않도록 진단 스택의 크기를 제한한다.
        event.addKeyValue("stack_truncated", framesTruncated || stack.length() > 24000 || current != null);
        event.addKeyValue(
                "stack_trace", stack.length() > 24000 ? stack.substring(0, 24000) + "\n... 생략" : stack.toString());
    }
}
