package com.donggree.global.metrics;

import com.donggree.global.apiPayload.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** 응답 본문을 복사하지 않고 서버가 정의한 업무 코드만 측정 필터에 전달한다. */
@ControllerAdvice
public class OperationMetricsAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest servlet
                && body instanceof ApiResponse<?> api
                && OperationMetricsFilter.operation(servlet.getServletRequest()) != null) {
            servlet.getServletRequest().setAttribute(OperationMetricsFilter.CODE, api.getCode());
            servlet.getServletRequest()
                    .setAttribute(
                            OperationMetricsFilter.OUTCOME,
                            api.getCode().startsWith("COMMON2") ? "success" : "failure");
        }
        return body;
    }
}
