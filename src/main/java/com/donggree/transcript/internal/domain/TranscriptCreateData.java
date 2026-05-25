package com.donggree.transcript.internal.domain;

import java.math.BigDecimal;

/**
 * Transcript 생성 시 PDF 파싱 결과를 전달하기 위한 파라미터 객체.
 */
public record TranscriptCreateData(
        Long memberId,
        String rawData,
        int admissionYear,
        String academicStatus,
        String studentType,
        Long departmentId,
        Long subMajor1Id,
        Long subMajor2Id,
        Long dualMajor1Id,
        Long dualMajor2Id,
        int totalCredits,
        BigDecimal gpa,
        int completedSemesters,
        String englishLevel,
        boolean engineeringCertified,
        boolean transfer,
        boolean selectiveCompletion,
        boolean globalTalentTrack,
        boolean englishCourseTarget,
        Boolean completedEnglishResult,
        Integer completedEnglishMajor,
        Integer completedEnglishNonMajor,
        Integer teachingAptitudeCount,
        boolean thesisStatus
) {
}
