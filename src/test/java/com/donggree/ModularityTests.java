package com.donggree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {
    @Test
    void verifyModularStructure() {
        // 모듈 간 의존성이 규칙에 맞는지 자동 검증
        // 예: transcript 모듈이 graduation 모듈의 internal 패키지에 접근하면 실패
        var modules = ApplicationModules.of(DonggreeApplication.class);
        modules.verify();
        var global = modules.getModuleByName("global").orElseThrow();
        assertThat(global.isOpen()).isFalse();
        assertThat(global.isExposed(com.donggree.global.auth.JwtTokenProvider.class))
                .isTrue();
        assertThat(global.isExposed(com.donggree.global.auth.JwtAuthFilter.class))
                .isFalse();
        assertThat(global.isExposed(com.donggree.global.metrics.OperationMetricsFilter.class))
                .isFalse();
        assertThat(global.isExposed(com.donggree.global.config.SecurityConfig.class))
                .isFalse();
    }

    @Test
    void 도메인과_응용_계층의_의존_방향을_검증한다() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.donggree");
        noClasses()
                .that()
                .resideInAPackage("..internal.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..internal.application..", "..internal.presentation..", "..internal.infrastructure..")
                .check(classes);
        noClasses()
                .that()
                .resideInAPackage("..internal.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..internal.presentation..", "..internal.infrastructure..")
                .check(classes);
    }
}
