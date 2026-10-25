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
                            .isEqualTo("3");
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

    // 실제 Modulith 자동 설정과 같은 기본 이벤트 엔티티만 포함한다. archive 모드는 사용하지 않는다.
    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackages = {"com.donggree", "org.springframework.modulith.events.jpa.updating"})
    static class Mappings {}
}
