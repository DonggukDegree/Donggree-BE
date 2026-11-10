package com.donggree.graduation.internal.application.readmodel;

import com.donggree.curriculum.CourseType;
import java.util.Map;

/** 일회성 리포트 요약·상세. 서버에 테스트 성적표나 미리보기 세션을 보관하지 않는다. */
public record ReportPreviewReadModel(GraduationReportReadModel report, Map<CourseType, AreaDetailReadModel> details) {}
