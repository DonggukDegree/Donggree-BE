package com.donggree.graduation.internal.domain.rule;

import java.util.List;

/** JSON 형식과 무관한 유형별 판정 설정. 누락값의 기존 의미는 각 판정기가 해석한다. */
public sealed interface RuleConfiguration {
    record RequiredCourse(
            List<String> courseCodes,
            List<List<List<String>>> requiredCourseSets,
            List<String> exemptEnglishLevels,
            List<String> requiredEnglishLevels)
            implements RuleConfiguration {}

    record Prerequisite(
            List<String> targetCourseCodes,
            List<String> prerequisiteCourseCodes,
            String conditionField,
            String conditionValue)
            implements RuleConfiguration {}

    record EnglishCourse(List<String> courseTypes, int minCount) implements RuleConfiguration {}

    record TeachingAptitude(int minCount) implements RuleConfiguration {}

    record Thesis(
            List<String> exemptStudentTypes,
            List<String> exemptCourseCodes,
            List<List<List<String>>> requiredCourseSets)
            implements RuleConfiguration {}

    record MinCredits(
            String courseType,
            List<String> areaNames,
            List<String> subCategories,
            List<String> pdfAreaNames,
            List<String> pdfCourseTypeNames,
            List<String> courseCodes,
            String creditSource,
            Integer minCredits,
            Integer minCount)
            implements RuleConfiguration {}

    record TotalCredits(int minCredits) implements RuleConfiguration {}

    record Gpa(double minGpa, String gpaScope) implements RuleConfiguration {}

    record NoSettings() implements RuleConfiguration {}
}
