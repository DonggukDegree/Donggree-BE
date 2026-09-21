package com.donggree.transcript;

import java.math.BigDecimal;
import java.util.List;

/**
 * transcript 모듈 외부에 성적표 전체 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * graduation 모듈이 졸업 판정에 필요한 모든 메타 정보와 수강 이력을 담는다.
 * dualMajor1ThesisStatus는 raw_data.meta의 복수1 논문·시험 결과가 명시적으로 합격인 경우에만 true다.
 */
public record TranscriptView(
        Long id,
        Long memberId,
        Long departmentId,
        Long dualMajor1Id,
        Long dualMajor2Id,
        Long subMajor1Id,
        Long subMajor2Id,
        int admissionYear,
        String studentType,
        boolean engineeringCertified,
        int totalCredits,
        BigDecimal gpa,
        String englishLevel,
        boolean englishCourseTarget,
        Boolean completedEnglishResult,
        Boolean englishPassResult,
        boolean thesisStatus,
        boolean dualMajor1ThesisStatus,
        boolean transfer,
        List<CourseRecordView> courseRecords) {}
