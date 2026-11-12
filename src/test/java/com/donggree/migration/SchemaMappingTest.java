package com.donggree.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@EnabledIfEnvironmentVariable(named = "DONGGREE_TEST_POSTGRES_URL", matches = ".+")
class SchemaMappingTest extends MigrationDatabase {
    @Test
    void Flyway_적용_직후_전체_엔티티와_이벤트_매핑을_validate한다() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        FlywayAutoConfiguration.class,
                        HibernateJpaAutoConfiguration.class,
                        TransactionAutoConfiguration.class))
                .withUserConfiguration(Mappings.class)
                .withPropertyValues(
                        "spring.datasource.url=" + databaseUrl,
                        "spring.datasource.username=" + user,
                        "spring.datasource.password=" + password,
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.flyway.enabled=true",
                        "spring.flyway.baseline-on-migrate=false",
                        "spring.flyway.clean-disabled=true",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.flyway.locations=classpath:db/migration")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(EntityManagerFactory.class).isOpen())
                            .isTrue();
                    assertThat(context.getBean(Flyway.class)
                                    .info()
                                    .current()
                                    .getVersion()
                                    .getVersion())
                            .isEqualTo("7");
                });
    }

    @Test
    void 운영의_동명_학과와_설정별_규칙_및_역할_정규화_제약을_보존한다() throws Exception {
        flyway().migrate();
        execute(
                """
                INSERT INTO public.college (college_name) VALUES ('migration-college-a'), ('migration-college-b');
                INSERT INTO public.department (college_id, department_name) VALUES (1, 'migration-department'), (2, 'migration-department');
                INSERT INTO public.rule_type (type_name, course_type) VALUES ('MIN_CREDITS', 'FIRST_MAJOR');
                INSERT INTO public.graduation_rule (rule_type_id, rule_name, rule_config) VALUES
                    (1, 'migration-rule', '{"minCredits":12,"applicableMajorRoles":["SINGLE_PRIMARY"]}'),
                    (1, 'migration-rule', '{"minCredits":24,"applicableMajorRoles":["SINGLE_PRIMARY"]}');
                """);
        assertThat(scalar("SELECT count(*) FROM public.department")).isEqualTo("2");
        assertThat(scalar("SELECT count(*) FROM public.graduation_rule")).isEqualTo("2");
        assertThatThrownBy(
                        () -> execute(
                                "INSERT INTO public.department (college_id, department_name) VALUES (1, 'migration-department')"))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23505");
        assertThatThrownBy(
                        () -> execute(
                                """
                INSERT INTO public.graduation_rule (rule_type_id, rule_name, rule_config)
                VALUES (1, 'non-canonical', '{"minCredits":12,"applicableMajorRoles":["SINGLE_PRIMARY","SECONDARY","SINGLE_PRIMARY"]}')
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23514");
        assertThat(
                        scalar(
                                """
                SELECT public.normalize_graduation_rule_roles(
                  '{"applicableMajorRoles":["SINGLE_PRIMARY","SECONDARY","SINGLE_PRIMARY"]}'::jsonb)::text
                """))
                .isEqualTo("{\"applicableMajorRoles\": [\"SECONDARY\", \"SINGLE_PRIMARY\"]}");
    }

    @Test
    void 장소_신고는_회원당_장소별_처리_대기_1건과_기타_설명_필수를_DB에서도_지킨다() throws Exception {
        flyway().migrate();
        execute(
                """
                INSERT INTO public.place (name, category, sub_category, latitude, longitude, status, version,
                    created_by, info_updated_at, created_at, updated_at)
                    VALUES ('migration-place', 'ETC', 'ETC', 37.558, 127.0, 'VISIBLE', 0, 1, now(), now(), now());
                INSERT INTO public.place_report (place_id, reporter_id, reason, status, created_at, updated_at)
                    VALUES (1, 7, 'CLOSED', 'PENDING', now(), now());
                """);
        assertThatThrownBy(
                        () -> execute(
                                """
                INSERT INTO public.place_report (place_id, reporter_id, reason, status, created_at, updated_at)
                    VALUES (1, 7, 'WRONG_NAME', 'PENDING', now(), now())
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23505");
        execute(
                """
                UPDATE public.place_report SET status = 'RESOLVED', processed_at = now();
                INSERT INTO public.place_report (place_id, reporter_id, reason, status, created_at, updated_at)
                    VALUES (1, 7, 'WRONG_NAME', 'PENDING', now(), now());
                """);
        assertThat(scalar("SELECT count(*) FROM public.place_report")).isEqualTo("2");
        assertThatThrownBy(
                        () -> execute(
                                """
                INSERT INTO public.place_report (place_id, reporter_id, reason, status, created_at, updated_at)
                    VALUES (1, 8, 'OTHER', 'PENDING', now(), now())
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23514");
    }

    @Test
    void 리뷰는_회원당_장소별_미삭제_1건과_별점_본문_제약을_DB에서도_지킨다() throws Exception {
        flyway().migrate();
        execute(
                """
                INSERT INTO public.place (name, category, sub_category, latitude, longitude, status, version,
                    created_by, info_updated_at, created_at, updated_at)
                    VALUES ('migration-place', 'ETC', 'ETC', 37.558, 127.0, 'VISIBLE', 0, 1, now(), now(), now());
                INSERT INTO public.place_review (place_id, author_id, rating, content, status, created_at, updated_at)
                    VALUES (1, 7, 5, 'migration-review-content', 'VISIBLE', now(), now());
                """);
        String duplicate =
                """
                INSERT INTO public.place_review (place_id, author_id, rating, content, status, created_at, updated_at)
                    VALUES (1, 7, 4, 'migration-review-again', 'VISIBLE', now(), now())
                """;
        assertThatThrownBy(() -> execute(duplicate))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23505");
        execute("UPDATE public.place_review SET deleted_at = now()");
        execute(duplicate);
        assertThat(scalar("SELECT count(*) FROM public.place_review")).isEqualTo("2");
        assertThatThrownBy(
                        () -> execute(
                                """
                INSERT INTO public.place_review (place_id, author_id, rating, content, status, created_at, updated_at)
                    VALUES (1, 8, 6, 'migration-review-content', 'VISIBLE', now(), now())
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23514");
        execute(
                """
                INSERT INTO public.place_review_report (review_id, reporter_id, reason, status, created_at, updated_at)
                    SELECT max(id), 8, 'ABUSE', 'PENDING', now(), now() FROM public.place_review;
                """);
        assertThatThrownBy(
                        () -> execute(
                                """
                INSERT INTO public.place_review_report (review_id, reporter_id, reason, status, created_at, updated_at)
                    SELECT max(id), 8, 'SPAM', 'PENDING', now(), now() FROM public.place_review
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23505");
    }

    @Test
    void V7은_없앤_상세카테고리를_기타로_옮기고_태그는_정의된_값만_받는다() throws Exception {
        var beforeV7 = org.flywaydb.core.Flyway.configure()
                .configuration(flyway().getConfiguration())
                .target("6")
                .load();
        beforeV7.migrate();
        execute(
                """
                INSERT INTO public.place (name, category, sub_category, latitude, longitude, status, version,
                    created_by, info_updated_at, created_at, updated_at)
                    VALUES ('migration-pocha', 'BAR', 'POCHA', 37.558, 127.0, 'VISIBLE', 0, 1, now(), now(), now()),
                           ('migration-study', 'STUDY', 'STUDY_FRIENDLY_CAFE', 37.558, 127.0, 'VISIBLE', 0, 1, now(), now(), now());
                """);
        flyway().migrate();

        assertThat(scalar("SELECT string_agg(sub_category, ',' ORDER BY id) FROM public.place"))
                .isEqualTo("BAR_ETC,STUDY_ETC");
        execute("INSERT INTO public.place_tag (place_id, tag) VALUES (1, 'SOLO_DINING'), (1, 'SHORTCUT')");
        assertThatThrownBy(() -> execute("INSERT INTO public.place_tag (place_id, tag) VALUES (1, 'UNKNOWN')"))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo("23514");
    }

    // 실제 Modulith 자동 설정과 같은 기본 이벤트 엔티티만 포함한다. archive 모드는 사용하지 않는다.
    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackages = {"com.donggree", "org.springframework.modulith.events.jpa.updating"})
    static class Mappings {}
}
