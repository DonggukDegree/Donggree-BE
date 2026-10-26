package com.donggree.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

@EnabledIfEnvironmentVariable(named = "DONGGREE_TEST_POSTGRES_URL", matches = ".+")
class DatabaseMigrationTest extends MigrationDatabase {
    @Test
    void 빈_DB에는_데이터_없이_최신_스키마를_적용하고_재시작_시_재실행하지_않는다() throws Exception {
        var flyway = flyway();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);
        for (String table : TABLES)
            assertThat(scalar("SELECT count(*) FROM public." + table)).isEqualTo("0");
        assertThat(scalar("SELECT max(version) FROM public.flyway_schema_history WHERE success AND type='SQL'"))
                .isEqualTo("3");
        assertThat(scalar("SELECT to_regclass('public.school_email_verification')"))
                .isNull();
        execute("INSERT INTO public.college (college_name) VALUES ('migration-test')");
        var before = dataSnapshot();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(dataSnapshot()).isEqualTo(before);
        flyway.validate();
    }

    @Test
    void 기존_DB는_명시적_baseline_후_데이터와_시퀀스를_보존한다() throws Exception {
        createExistingSchema();
        execute(
                """
                INSERT INTO public.college (college_name) VALUES ('migration-test');
                SELECT setval('public.college_id_seq', 9000, true);
                INSERT INTO public.member (created_at, updated_at, email, identity_verified, oauth_id, role)
                    VALUES (now(), now(), 'migration-test@example.invalid', false, 'migration-test', 'STUDENT');
                INSERT INTO public.transcript (created_at, updated_at, academic_status, admission_year,
                    completed_semesters, engineering_certified, english_course_target, global_talent_track,
                    gpa, member_id, raw_data, selective_completion, thesis_status, total_credits, is_transfer)
                    VALUES (now(), now(), '재학', 2023, 0, false, false, false, 0, 1,
                    '{"migration":"preserve"}', false, false, 0, false);
                INSERT INTO public.faq (tag, title, content) VALUES ('SERVICE', 'migration-test', 'preserve');
                """);
        var before = dataSnapshot();
        var flyway = flyway();
        flyway.baseline();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(dataSnapshot()).isEqualTo(before);
        assertThat(scalar("SELECT last_value FROM public.college_id_seq")).isEqualTo("9000");
        assertThat(scalar("SELECT nextval('public.college_id_seq')")).isEqualTo("9001");
        assertThat(scalar("SELECT version FROM public.flyway_schema_history WHERE type='BASELINE' AND success"))
                .isEqualTo("1");
    }

    @Test
    void 기존_DB를_자동_baseline하거나_초기화하지_않는다() throws Exception {
        createExistingSchema();
        execute("INSERT INTO public.college (college_name) VALUES ('migration-test')");
        var before = dataSnapshot();
        assertThatThrownBy(() -> flyway().migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");
        assertThatThrownBy(() -> flyway().clean())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("cleanDisabled");
        assertThat(dataSnapshot()).isEqualTo(before);
    }

    @Test
    void 적용된_V1의_체크섬_변경을_거부한다(@TempDir Path changed) throws Exception {
        flyway().migrate();
        var original = new ClassPathResource("db/migration/V1__baseline_schema.sql");
        try (var input = original.getInputStream()) {
            Files.write(changed.resolve("V1__baseline_schema.sql"), input.readAllBytes());
        }
        Files.writeString(
                changed.resolve("V1__baseline_schema.sql"),
                "\n-- modified migration\n",
                java.nio.file.StandardOpenOption.APPEND);
        var invalid = Flyway.configure()
                .dataSource(databaseUrl, user, password)
                .locations("filesystem:" + changed)
                .defaultSchema("public")
                .load();
        assertThatThrownBy(invalid::validate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("checksum");
        assertThat(scalar("SELECT count(*) FROM public.flyway_schema_history WHERE success"))
                .isEqualTo("3");
    }
}
