package com.donggree.testsupport;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트 JVM 전체가 함께 쓰는 PostgreSQL 16 컨테이너. 처음 필요할 때 한 번만 띄우고, JVM이 끝나면 Testcontainers가 정리한다.
 * 테스트끼리 데이터가 섞이지 않도록 Spring 컨텍스트·마이그레이션 테스트마다 {@link #createDatabase}로 새 DB를 만들어 쓴다.
 */
public final class TestPostgres {

    private static final String IMAGE = "postgres:16";
    // 테스트 컨텍스트(최대 32개 캐시)가 각자 커넥션 풀을 잡으므로 기본 상한 100보다 넉넉히 연다.
    private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE))
            .withCommand("postgres", "-c", "max_connections=300");

    private TestPostgres() {}

    private static synchronized PostgreSQLContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    /** DB 생성·삭제용 관리 DB(postgres) 주소. 컨테이너 기본 사용자는 슈퍼유저라 DB를 만들 수 있다. */
    public static String adminUrl() {
        return urlOf("postgres");
    }

    public static String urlOf(String databaseName) {
        PostgreSQLContainer<?> container = container();
        return "jdbc:postgresql://" + container.getHost() + ":"
                + container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + databaseName;
    }

    public static String username() {
        return container().getUsername();
    }

    public static String password() {
        return container().getPassword();
    }

    /** prefix 뒤에 무작위 값을 붙인 빈 DB를 만들고 이름을 돌려준다. */
    public static String createDatabase(String prefix) {
        String name = prefix + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(adminUrl(), username(), password());
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException exception) {
            throw new IllegalStateException("테스트 DB를 만들지 못했습니다: " + name, exception);
        }
        return name;
    }
}
