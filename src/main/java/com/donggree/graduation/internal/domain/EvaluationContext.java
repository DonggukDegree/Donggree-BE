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
 * transcript 데이터와 course_code별 분류(classificationByCourseCode)를 함께 보관하며,
 * evaluator가 자주 사용하는 필터링 연산을 편의 메서드로 제공한다.
 *
 * classificationByCourseCode 맵은 서비스 레이어에서 아래 순서로 조립한다:
 *   1. course_record의 course_code 목록으로 Course 카탈로그 조회
 *   2. course.id 목록과 입학년도로 CourseClassification 조회
 *   3. courseCode → CourseClassificationView 맵 구성
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
     * 특정 courseType에서 지정한 subCategory 목록에 속하는 이수 수강 이력을 반환한다.
     * subCategories가 null이면 subCategory 제한 없이 해당 courseType 전체를 반환한다.
     */
    public List<CourseRecordView> getPassedCoursesByTypeAndSubCategories(
            CourseType courseType, List<String> subCategories) {
        return getPassedCourses().stream()
                .filter(cr -> {
                    CourseClassificationView cls = classificationByCourseCode.get(cr.courseCode());
                    if (cls == null || cls.courseType() != courseType) return false;
                    return subCategories == null || subCategories.contains(cls.subCategory());
                })
                .toList();
    }

    /** 주어진 과목명을 이수한 과목이 있는지 확인한다. */
    public boolean hasPassedCourseByName(String courseName) {
        return getPassedCourses().stream().anyMatch(cr -> courseName.equals(cr.courseName()));
    }

    /**
     * 주어진 과목명을 이수한 가장 이른 학기를 반환한다.
     * 선이수체계 판정 시 이수 순서 비교에 사용한다.
     */
    public Optional<String> getEarliestSemesterByCourseName(String courseName) {
        return getPassedCourses().stream()
                .filter(cr -> courseName.equals(cr.courseName()))
                .map(CourseRecordView::semester)
                .min(Comparator.comparingInt(EvaluationContext::semesterOrdinal));
    }

    /** 특정 courseType의 이수 학점 합계를 반환한다. */
    public int getTotalPassedCreditsByType(CourseType courseType) {
        return getPassedCoursesByType(courseType).stream()
                .mapToInt(CourseRecordView::credits)
                .sum();
    }

    /** 특정 courseType + subCategory 목록의 이수 학점 합계를 반환한다. */
    public int getTotalPassedCreditsByTypeAndSubCategories(CourseType courseType, List<String> subCategories) {
        return getPassedCoursesByTypeAndSubCategories(courseType, subCategories).stream()
                .mapToInt(CourseRecordView::credits)
                .sum();
    }

    /**
     * 학기 문자열을 정수 서수로 변환한다.
     * "YYYY-1" → YYYY*100+1, "YYYY-하/동" → YYYY*100+5, "YYYY-2" → YYYY*100+10
     * 선이수체계 판정에서 이수 순서 비교용으로만 사용한다.
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
