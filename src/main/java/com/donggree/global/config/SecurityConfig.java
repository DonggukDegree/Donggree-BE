package com.donggree.global.config;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralErrorCode;
import com.donggree.global.auth.JwtAuthFilter;
import com.donggree.global.auth.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정.
 * OAuth2 로그인(카카오)과 JWT 인증 필터를 조합한다.
 * OAuth2 로그인 시 임시 세션을 사용하고, 이후 API 요청은 JWT로 인증한다.
 * CORS는 app.cors.allowed-origins 프로퍼티에서 허용 Origin을 읽어 설정한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private static final String[] PERMIT_URIS = {
        "/auth/refresh",
        "/oauth2/authorization/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/docs/**",
        "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService,
            AuthenticationSuccessHandler oAuthSuccessHandler,
            AuthenticationFailureHandler oAuthFailureHandler,
            ObjectMapper objectMapper,
            Environment env)
            throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtTokenProvider);
        boolean isLocal = Arrays.asList(env.getActiveProfiles()).contains("local");
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // API 인증은 JWT(Bearer)로만 한다. OAuth 로그인 성공 인증을 세션에 저장/복원하지 않아
                // 이후 요청이 OAuthMember 세션으로 인증되는 것을 막는다. (OAuth 핸드셰이크용 세션은 별개로 유지)
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PERMIT_URIS).permitAll();
                    if (isLocal) {
                        auth.requestMatchers("/auth/test-login").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                // 인증/인가 실패도 일반 API와 동일한 ApiResponse 봉투(JSON)로 응답한다.
                // (이전에는 sendError로 Spring 기본 에러 JSON이 나가 프론트가 code로 분기할 수 없었다.)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, GeneralErrorCode.UNAUTHORIZED, objectMapper))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, GeneralErrorCode.FORBIDDEN, objectMapper)))
                .oauth2Login(oauth -> oauth.redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth/callback/*"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuthSuccessHandler)
                        .failureHandler(oAuthFailureHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증/인가 실패 시 ApiResponse 봉투를 JSON으로 직접 응답에 기록한다.
     * 일반 API의 GeneralExceptionAdvice 응답과 동일한 형식을 유지하기 위함이다.
     */
    private void writeErrorResponse(HttpServletResponse response, GeneralErrorCode code, ObjectMapper objectMapper)
            throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.onFailure(code));
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
