package com.donggree.graduation.internal.infrastructure.evaluation;

import com.donggree.graduation.internal.domain.evaluator.courses.PrerequisiteEvaluator;
import com.donggree.graduation.internal.domain.evaluator.courses.RequiredCourseEvaluator;
import com.donggree.graduation.internal.domain.evaluator.courses.ScienceConflictEvaluator;
import com.donggree.graduation.internal.domain.evaluator.credits.GpaEvaluator;
import com.donggree.graduation.internal.domain.evaluator.credits.MinCreditsEvaluator;
import com.donggree.graduation.internal.domain.evaluator.credits.TotalCreditsEvaluator;
import com.donggree.graduation.internal.domain.evaluator.qualification.EnglishCourseEvaluator;
import com.donggree.graduation.internal.domain.evaluator.qualification.TeachingAptitudeEvaluator;
import com.donggree.graduation.internal.domain.evaluator.qualification.ThesisEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 순수 판정 객체를 Spring에 연결하는 구성. */
@Configuration(proxyBeanMethods = false)
public class RuleEvaluatorConfiguration {
    @Bean
    RequiredCourseEvaluator requiredCourseEvaluator() {
        return new RequiredCourseEvaluator();
    }

    @Bean
    ScienceConflictEvaluator scienceConflictEvaluator() {
        return new ScienceConflictEvaluator();
    }

    @Bean
    PrerequisiteEvaluator prerequisiteEvaluator() {
        return new PrerequisiteEvaluator();
    }

    @Bean
    EnglishCourseEvaluator englishCourseEvaluator() {
        return new EnglishCourseEvaluator();
    }

    @Bean
    TeachingAptitudeEvaluator teachingAptitudeEvaluator() {
        return new TeachingAptitudeEvaluator();
    }

    @Bean
    ThesisEvaluator thesisEvaluator() {
        return new ThesisEvaluator();
    }

    @Bean
    MinCreditsEvaluator minCreditsEvaluator() {
        return new MinCreditsEvaluator();
    }

    @Bean
    TotalCreditsEvaluator totalCreditsEvaluator() {
        return new TotalCreditsEvaluator();
    }

    @Bean
    GpaEvaluator gpaEvaluator() {
        return new GpaEvaluator();
    }
}
