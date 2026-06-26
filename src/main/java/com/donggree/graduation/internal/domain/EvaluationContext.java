package com.donggree.graduation.internal.domain;

import com.donggree.curriculum.CourseClassificationView;
import com.donggree.curriculum.CourseType;
import com.donggree.transcript.CourseRecordView;
import com.donggree.transcript.TranscriptView;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 졸업 판정에 필요한 데이터를 묶는 값 객체.
 * transcript 데이터와 courseCode별 분류를 함께 보관하며,
 * evaluator가 자주 사용하는 필터링 연산을 편의 메서드로 제공한다.
 */
public class EvaluationContext {

    private final TranscriptView transcript;
    private final Map<String, CourseClassificationView> classificationByCourseCode;

    public EvaluationContext(
            TranscriptView transcript, Map<String, CourseClassificationView> classificationByCourseCode) {
        this.transcript = transcript;
        this.classificationByCourseCode = Map.copyOf(classificationByCourseCode);
    }

    public TranscriptView getTranscript() {
        return transcript;
    }

    public CourseClassificationView getClassification(String courseCode) {
        return classificationByCourseCode.get(courseCode);
    }

    /** 이수 처리된(F·NP 제외) 수강 이력 전체를 반환한다. */
    public List<CourseRecordView> getPassedCourses() {
        return transcript.courseRecords().stream()
                .filter(CourseRecordView::passed)
                .toList();
    }

    /** 특정 courseType의 이수 수강 이력을 반환한다. 분류가 없는 과목은 제외된다. */
    public List<CourseRecordView> getPassedCoursesByType(CourseType courseType) {
        return getPassedCourses().stream()
                .filter(cr -> {
                    CourseClassificationView cls = classificationByCourseCode.get(cr.courseCode());
                    return cls != null && cls.courseType() == courseType;
                })
                .toList();
    }

    /**
     * 특정 courseType에서 지정한 areaName 목록에 속하는 이수 수강 이력을 반환한다.
     * areaNames가 null이면 영역 제한 없이 해당 courseType 전체를 반환한다.
     */
    public List<CourseRecordView> getPassedCoursesByTypeAndAreaNames(CourseType courseType, List<String> areaNames) {
        return getPassedCourses().stream()
                .filter(cr -> {
                    CourseClassificationView cls = classificationByCourseCode.get(cr.courseCode());
                    if (cls == null || cls.courseType() != courseType) return false;
                    return areaNames == null || areaNames.contains(cls.areaName());
                })
                .toList();
    }

    /** ThesisEvaluator에서 사용. 과목명으로 이수 여부를 확인한다. */
    public boolean hasPassedCourseByName(String courseName) {
        return getPassedCourses().stream().anyMatch(cr -> courseName.equals(cr.courseName()));
    }

    /**
     * 주어진 코드 패턴 목록 중 하나라도 이수했는지 확인한다.
     * 단일 코드면 1개짜리 리스트, 동일유사 교과목이면 여러 코드 리스트를 넘긴다.
     * "DAI*" 처럼 '*'로 끝나는 패턴은 prefix 매칭으로 처리한다.
     */
    public boolean hasPassedAnyCourseByCode(List<String> patterns) {
        return getPassedCourses().stream()
                .anyMatch(cr -> cr.courseCode() != null && matchesAny(cr.courseCode(), patterns));
    }

    /**
     * 주어진 코드 패턴 목록 중 가장 이른 이수 학기를 반환한다.
     * 선이수체계 판정 시 이수 순서 비교에 사용한다. prefix 패턴 지원.
     */
    public Optional<String> getEarliestSemesterByAnyCourseCode(List<String> patterns) {
        return getPassedCourses().stream()
                .filter(cr -> cr.courseCode() != null && matchesAny(cr.courseCode(), patterns))
                .map(CourseRecordView::semester)
                .min(Comparator.comparingInt(EvaluationContext::semesterOrdinal));
    }

    /** '*' 로 끝나면 prefix 매칭, 아니면 exact 매칭. */
    private static boolean matchesAny(String courseCode, List<String> patterns) {
        return patterns.stream()
                .anyMatch(p ->
                        p.endsWith("*") ? courseCode.startsWith(p.substring(0, p.length() - 1)) : courseCode.equals(p));
    }

    /** 특정 courseType의 이수 학점 합계를 반환한다. */
    public int getTotalPassedCreditsByType(CourseType courseType) {
        return getPassedCoursesByType(courseType).stream()
                .mapToInt(CourseRecordView::credits)
                .sum();
    }

    /** 특정 courseType + areaName 목록의 이수 학점 합계를 반환한다. */
    public int getTotalPassedCreditsByTypeAndAreaNames(CourseType courseType, List<String> areaNames) {
        return getPassedCoursesByTypeAndAreaNames(courseType, areaNames).stream()
                .mapToInt(CourseRecordView::credits)
                .sum();
    }

    /**
     * 학기 문자열을 정수 서수로 변환한다.
     * "YYYY-1" → YYYY*100+1, "YYYY-하/동" → YYYY*100+5, "YYYY-2" → YYYY*100+10
     */
    public static int semesterOrdinal(String semester) {
        if (semester == null) return 0;
        String[] parts = semester.split("-");
        if (parts.length != 2) return 0;
        try {
            int year = Integer.parseInt(parts[0]);
            int term =
                    switch (parts[1]) {
                        case "1" -> 1;
                        case "하", "동" -> 5;
                        case "2" -> 10;
                        default -> 0;
                    };
            return year * 100 + term;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
