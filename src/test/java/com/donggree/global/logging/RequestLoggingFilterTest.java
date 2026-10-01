package com.donggree.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.handler.GeneralExceptionAdvice;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

class RequestLoggingFilterTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestDiagnostics.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 처리된_업무오류는_한번만_기록하고_응답ID와_원인위치를_연결한다() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(new GeneralExceptionAdvice())
                .addFilters(filter)
                .build();
        var result = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/test/123")
                                .queryParam("token", "QUERY_SECRET")
                                .header("Authorization", "Bearer TOKEN_SECRET")
                                .header("X-Request-ID", "CLIENT_SECRET"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        Map<String, Object> fields = fields(event);
        String requestId = result.getResponse().getHeader("X-Request-ID");
        assertThat(requestId).matches("[a-f0-9-]{36}");
        assertThat(event.getMDCPropertyMap()).containsEntry("request_id", requestId);
        assertThat(fields)
                .containsEntry("endpoint", "/api/test/{id}")
                .containsEntry("stage", "pdf_json_encode")
                .containsEntry("code", "COMMON500_1")
                .containsEntry("http_status", 500)
                .containsEntry("root_exception_type", SQLException.class.getName())
                .containsEntry("sql_state", "23505");
        String json = encode(event);
        assertThat(json)
                .contains("request_id", "pdf_json_encode", "stack_trace", "23505", "FailureController")
                .doesNotContain(
                        "QUERY_SECRET",
                        "TOKEN_SECRET",
                        "CLIENT_SECRET",
                        "PRIVATE_PDF_SQL_VALUE",
                        "private@example.com");
        assertThat(MDC.get("request_id")).isNull();
    }

    @Test
    void 필터예외는_원본그대로_전파하며_MDC복원_및_민감경로제외() {
        MDC.put("previous", "preserved");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private@example.com/TOKEN_SECRET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletException failure = new ServletException("TOKEN_SECRET");
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
                    throw failure;
                }))
                .isSameAs(failure);
        assertThat(appender.list).hasSize(1);
        assertThat(fields(appender.list.getFirst()))
                .containsEntry("endpoint", "UNMATCHED")
                .containsEntry("exception_escaped", true);
        assertThat(encode(appender.list.getFirst())).doesNotContain("TOKEN_SECRET", "private@example.com");
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(Map.of("previous", "preserved"));
    }

    @Test
    void 정상요청은_오류로그없이_ID만발급하고_다음요청과_분리한다() throws Exception {
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/faqs"), first, (req, res) -> {});
        filter.doFilter(new MockHttpServletRequest("GET", "/api/faqs"), second, (req, res) -> {});
        assertThat(first.getHeader("X-Request-ID")).isNotEqualTo(second.getHeader("X-Request-ID"));
        assertThat(appender.list).isEmpty();
    }

    @Test
    void 보안필터의_인증실패와_관리자권한거절도_기록한다() throws Exception {
        for (int status : new int[] {401, 403}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/courses");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> response.setStatus(status));
        }
        assertThat(appender.list).hasSize(2);
        assertThat(fields(appender.list.getFirst())).containsEntry("code", "HTTP_401");
        assertThat(fields(appender.list.getLast()))
                .containsEntry("code", "HTTP_403")
                .containsEntry("endpoint", "/api/admin/**");
    }

    @Test
    void OAuth실패는_302여도_실패로기록한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth/callback/kakao");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            RequestDiagnostics.failure(
                    request,
                    "LOGIN_FAILURE",
                    "oauth_callback",
                    "카카오 인증 처리 실패",
                    new IllegalStateException("TOKEN_SECRET"));
            response.sendRedirect("https://example.test/login?error=login_failed");
        });
        assertThat(appender.list).hasSize(1);
        assertThat(fields(appender.list.getFirst()))
                .containsEntry("code", "LOGIN_FAILURE")
                .containsEntry("http_status", 302);
    }

    @Test
    void 데이터대체경고는_같은요청단계에서_한번만_기록하고_원문은_제외한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reports/summary");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/reports/summary");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            RequestDiagnostics.fallback("transcript_meta_parse", new IllegalArgumentException("PRIVATE_PDF_SQL_VALUE"));
            RequestDiagnostics.fallback("transcript_meta_parse", new IllegalArgumentException("PRIVATE_PDF_SQL_VALUE"));
        });
        assertThat(appender.list).hasSize(1);
        assertThat(fields(appender.list.getFirst())).containsEntry("event", "data_fallback");
        assertThat(encode(appender.list.getFirst())).doesNotContain("PRIVATE_PDF_SQL_VALUE");
    }

    @Test
    void 입력검증_필드명과_JSON오류위치를_남기되_입력값은_남기지_않는다() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(new GeneralExceptionAdvice())
                .addFilters(filter)
                .build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/test")
                        .contentType("application/json")
                        .content("{\"name\":\"PRIVATE_NAME\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isBadRequest());
        assertThat(fields(appender.list.getFirst()))
                .containsEntry("code", "VALID400_1")
                .containsEntry("validation_fields", java.util.List.of("email"));
        assertThat(encode(appender.list.getFirst())).doesNotContain("PRIVATE_NAME");
        appender.list.clear();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/test")
                        .contentType("application/json")
                        .content("{\"name\": PRIVATE_NAME}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isBadRequest());
        assertThat(fields(appender.list.getFirst()))
                .containsEntry("stage", "request_binding")
                .containsKeys("json_line", "json_column");
        assertThat(encode(appender.list.getFirst())).doesNotContain("PRIVATE_NAME");
    }

    @Test
    void 로깅이_실패해도_업무응답을_변경하지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException badDiagnostic = new RuntimeException() {
            @Override
            public StackTraceElement[] getStackTrace() {
                throw new IllegalStateException("로깅 실패 모사");
            }
        };
        filter.doFilter(request, response, (req, res) -> {
            RequestDiagnostics.failure(request, "COMMON500_1", "application", "서버 오류", badDiagnostic);
            response.setStatus(500);
            response.getWriter().write("original response");
        });
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).isEqualTo("original response");
        assertThat(MDC.get("request_id")).isNull();
    }

    private static Map<String, Object> fields(ILoggingEvent event) {
        return event.getKeyValuePairs().stream().collect(Collectors.toMap(k -> k.key, k -> k.value));
    }

    private String encode(ILoggingEvent event) {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.start();
        try {
            return new String(encoder.encode(event), StandardCharsets.UTF_8);
        } finally {
            encoder.stop();
        }
    }

    @RestController
    static class FailureController {
        record Input(String name, @jakarta.validation.constraints.NotBlank String email) {}

        @org.springframework.web.bind.annotation.PostMapping("/api/test")
        public String validate(
                @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody Input input) {
            return "ok";
        }

        @GetMapping("/api/test/{id}")
        public String fail() {
            throw new GeneralException(
                    GeneralErrorCode.INTERNAL_SERVER_ERROR,
                    new SQLException("PRIVATE_PDF_SQL_VALUE private@example.com", "23505"),
                    "pdf_json_encode");
        }
    }
}
