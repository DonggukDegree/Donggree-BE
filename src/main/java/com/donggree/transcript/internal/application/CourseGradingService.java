package com.donggree.transcript.internal.application;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.curriculum.GradingSystem;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 과목코드·교육과정 적용년도에 대해 관리자가 확인한 P/F 분류만 사용한다. */
@Service
@RequiredArgsConstructor
public class CourseGradingService {
    private final CurriculumLookupService curriculum;

    public Set<String> passFailCodes(List<String> codes, int year) {
        return curriculum.findCourseClassifications(codes.stream().distinct().toList(), year).entrySet().stream()
                .filter(e -> e.getValue().gradingSystem() == GradingSystem.PASS_FAIL)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
