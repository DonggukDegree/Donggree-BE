package com.donggree.transcript;

import java.math.BigDecimal;
import java.util.List;

/**
 * transcript 모듈 외부에 성적표 전체 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * graduation 모듈이 졸업 판정에 필요한 모든 메타 정보와 수강 이력을 담는다.
 */
public record TranscriptView(
        Long id,
        Long memberId,
        Long departmentId,
        int admissionYear,
        String studentType,
        int totalCredits,
        BigDecimal gpa,
        String englishLevel,
        boolean englishCourseTarget,
        Boolean completedEnglishResult,
        boolean thesisStatus,
        List<CourseRecordView> courseRecords) {}
