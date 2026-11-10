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

    @Test
    void 졸업_판정_도메인은_외부_DTO와_기술_라이브러리에_의존하지_않는다() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.donggree.graduation.internal.domain");
        noClasses()
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("View")
                .check(classes);
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("com.fasterxml.jackson..", "org.springframework..", "jakarta.persistence..")
                .check(classes);
    }

    @Test
    void HTTP_응답은_중첩_필드까지_응용_모델과_모듈_View를_노출하지_않는다() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.donggree");
        for (var type : classes) {
            if (type.getPackageName().contains(".internal.presentation.dto")
                    && type.getSimpleName().endsWith("Response")) {
                verifyResponseType(type.reflect(), new java.util.HashSet<>());
            }
        }
        for (var method :
                com.donggree.graduation.internal.presentation.GraduationReportController.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                verifyResponseType(method.getGenericReturnType(), new java.util.HashSet<>());
            }
        }
    }

    private static void verifyResponseType(java.lang.reflect.Type type, java.util.Set<java.lang.reflect.Type> visited) {
        if (!visited.add(type)) return;
        if (type instanceof java.lang.reflect.ParameterizedType parameterized) {
            verifyResponseType(parameterized.getRawType(), visited);
            for (var argument : parameterized.getActualTypeArguments()) verifyResponseType(argument, visited);
        } else if (type instanceof Class<?> valueType) {
            assertThat(valueType.getPackageName()).doesNotContain(".internal.application.");
            assertThat(valueType.getSimpleName()).doesNotEndWith("View");
            if (valueType.isRecord()) {
                for (var component : valueType.getRecordComponents())
                    verifyResponseType(component.getGenericType(), visited);
            }
        }
    }
}
