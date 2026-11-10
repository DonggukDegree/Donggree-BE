package com.donggree.transcript.internal.application;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 저장이 커밋된 PDF의 품질을 관측한다. 학생 정보·파일명·PDF 원문은 기록하지 않는다. */
@Component
@RequiredArgsConstructor
public class TranscriptUploadTelemetry {
    private final MeterRegistry registry;

    public void saved(long fileBytes, int courseCount, int creditGap, int gradeReviewCount, boolean departmentMapped) {
        try {
            String quality = courseCount == 0
                    ? "empty_courses"
                    : creditGap != 0
                            ? gradeReviewCount > 0 ? "both" : "credit_gap"
                            : gradeReviewCount > 0 ? "grade_review" : "clean";
            registry.counter(
                            "donggree.transcript.upload",
                            "quality",
                            quality,
                            "department",
                            departmentMapped ? "mapped" : "unmapped")
                    .increment();
            var logger = LoggerFactory.getLogger(TranscriptUploadTelemetry.class);
            var event = "clean".equals(quality) && departmentMapped ? logger.atInfo() : logger.atWarn();
            event.addKeyValue("event", "transcript_upload")
                    .addKeyValue("quality", quality)
                    .addKeyValue("file_bytes", fileBytes)
                    .addKeyValue("course_count", courseCount)
                    .addKeyValue("credit_gap", creditGap)
                    .addKeyValue("grade_review_count", gradeReviewCount)
                    .addKeyValue("department_mapped", departmentMapped)
                    .log("성적표 저장 품질");
        } catch (RuntimeException ignored) {
            // 관측 실패가 이미 저장된 성적표 응답을 바꾸지 않는다.
        }
    }
}
