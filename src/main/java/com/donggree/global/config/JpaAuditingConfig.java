package com.donggree.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * - @WebMvcTest 등 슬라이스 테스트에서 불필요한 JPA 컨텍스트 로드를 방지하기 위해
 *   메인 애플리케이션 클래스가 아닌 별도 Configuration으로 분리
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
