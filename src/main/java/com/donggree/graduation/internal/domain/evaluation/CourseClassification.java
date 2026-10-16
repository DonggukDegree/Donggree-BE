package com.donggree.graduation.internal.domain.evaluation;

import com.donggree.curriculum.CourseType;

/** 판정에 사용하는 과목 분류. CourseType은 모듈 사이에 합의된 공개 값 타입이다. */
public record CourseClassification(CourseType courseType, String areaName, String subCategory, String subjectDomain) {}
