package com.donggree.testsupport;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 테스트용 Spring 컨텍스트마다 공용 PostgreSQL 컨테이너에 새 DB를 만들어 datasource로 연결한다.
 * 예전 H2처럼 컨텍스트끼리 스키마·데이터가 섞이지 않고, 각 DB에는 Flyway가 V1부터 마이그레이션을 적용한다.
 * src/test/resources/META-INF/spring.factories로 등록하므로 테스트 클래스에 따로 붙일 것이 없다.
 */
public class PostgresTestDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE = "donggreeTestPostgres";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE)) {
            return;
        }
        String database = TestPostgres.createDatabase("donggree_test_");
        environment
                .getPropertySources()
                .addFirst(new MapPropertySource(
                        PROPERTY_SOURCE,
                        Map.of(
                                "spring.datasource.url", TestPostgres.urlOf(database),
                                "spring.datasource.username", TestPostgres.username(),
                                "spring.datasource.password", TestPostgres.password(),
                                "spring.datasource.driver-class-name", "org.postgresql.Driver")));
    }
}
