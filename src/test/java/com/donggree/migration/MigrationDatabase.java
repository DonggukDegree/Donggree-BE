package com.donggree.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** 로컬 PostgreSQL에서 이 테스트가 생성한 임시 DB만 사용하고 정리한다. */
abstract class MigrationDatabase {
    static final List<String> TABLES = List.of(
            "area_type",
            "college",
            "course_classification",
            "course_record",
            "department",
            "event_publication",
            "faq",
            "graduation_rule",
            "member",
            "requirement_set",
            "requirement_set_rule",
            "rule_type",
            "transcript");

    String databaseUrl;
    String user;
    String password;
    private String adminUrl;
    private String databaseName;
    private boolean created;

    @BeforeEach
    void createDatabase() throws SQLException {
        adminUrl = System.getenv("DONGGREE_TEST_POSTGRES_URL");
        assertThat(adminUrl).startsWith("jdbc:postgresql://").endsWith("/postgres");
        assertThat(URI.create(adminUrl.substring("jdbc:".length())).getHost()).isIn("127.0.0.1", "localhost");
        user = System.getenv("DONGGREE_TEST_POSTGRES_USER");
        password = System.getenv("DONGGREE_TEST_POSTGRES_PASSWORD");
        databaseName = "donggree_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        databaseUrl = adminUrl.substring(0, adminUrl.lastIndexOf('/') + 1) + databaseName;
        try (var connection = DriverManager.getConnection(adminUrl, user, password);
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
            created = true;
        }
    }

    @AfterEach
    void removeCreatedDatabase() throws SQLException {
        if (!created) return;
        assertThat(databaseName).matches("donggree_migration_test_[0-9a-f]{32}");
        try (var connection = DriverManager.getConnection(adminUrl, user, password);
                var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE " + databaseName + " WITH (FORCE)");
        }
    }

    Flyway flyway() {
        return Flyway.configure()
                .dataSource(databaseUrl, user, password)
                .defaultSchema("public")
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .validateMigrationNaming(true)
                .load();
    }

    Connection connection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, user, password);
    }

    void createExistingSchema() throws SQLException {
        try (var connection = connection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__baseline_schema.sql"));
        }
    }

    void execute(String sql) throws SQLException {
        try (var connection = connection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    String scalar(String sql) throws SQLException {
        try (var connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    Map<String, String> dataSnapshot() throws SQLException {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String table : TABLES) {
            snapshot.put(
                    table,
                    scalar(
                            "SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text), '[]'::jsonb)::text FROM public."
                                    + table + " t"));
        }
        return snapshot;
    }
}
