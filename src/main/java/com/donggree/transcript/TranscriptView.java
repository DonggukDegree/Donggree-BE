package com.donggree.transcript;

import java.math.BigDecimal;
import java.util.List;

/**
 * transcript 모듈 외부에 성적표 전체 데이터를 노출하기 위한 공개 읽기 전용 DTO.
 * graduation 모듈이 졸업 판정에 필요한 모든 메타 정보와 수강 이력을 담는다.
 * dualMajor1ThesisStatus는 raw_data.meta의 복수1 논문·시험 결과가 명시적으로 합격인 경우에만 true다.
 * 학과 ID를 찾지 못해도 PDF의 복수전공 여부와 미해결 추가전공 경고는 보존한다. DB 컬럼은 추가하지 않는다.
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
        boolean dualMajorDeclared,
        boolean unresolvedAdditionalMajor,
        List<CourseRecordView> courseRecords,
        BigDecimal majorGpa,
        Integer teachingAptitudeCount) {

    /** 기존 호출부와 테스트 픽스처의 판정 입력 호환성 유지용 생성자. */
    public TranscriptView(
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
            boolean dualMajorDeclared,
            boolean unresolvedAdditionalMajor,
            List<CourseRecordView> courseRecords) {
        this(
                id,
                memberId,
                departmentId,
                dualMajor1Id,
                dualMajor2Id,
                subMajor1Id,
                subMajor2Id,
                admissionYear,
                studentType,
                engineeringCertified,
                totalCredits,
                gpa,
                englishLevel,
                englishCourseTarget,
                completedEnglishResult,
                englishPassResult,
                thesisStatus,
                dualMajor1ThesisStatus,
                transfer,
                dualMajorDeclared,
                unresolvedAdditionalMajor,
                courseRecords,
                null,
                null);
    }
}
