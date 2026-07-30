package com.donggree.transcript.internal.application;

import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawCourseRecord;
import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawSemesterGroup;
import com.donggree.transcript.internal.domain.CourseRecord;
import com.donggree.transcript.internal.domain.Transcript;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 성적표의 수강 이력을 학기 오름차순으로 그룹핑해 읽기용 구조로 변환하는 공유 헬퍼.
 * 조회(Query)와 수강 이력 치환 결과(Command) 응답이 동일한 변환을 공유한다.
 */
final class TranscriptRecordGrouper {

    private TranscriptRecordGrouper() {}

    static List<RawSemesterGroup> groupBySemester(Transcript transcript) {
        return transcript.getCourseRecords().stream()
                .sorted((a, b) -> a.getSemester().compareTo(b.getSemester()))
                .collect(Collectors.groupingBy(CourseRecord::getSemester, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(e -> new RawSemesterGroup(
                        e.getKey(),
                        e.getValue().stream()
                                .map(r -> new RawCourseRecord(
                                        r.getId(),
                                        r.getCourseCode(),
                                        r.getCourseName(),
                                        r.getCredits(),
                                        r.getAreaName(),
                                        r.getCourseTypeName(),
                                        r.getGrade().getValue(),
                                        r.isRetake()))
                                .toList()))
                .toList();
    }
}
