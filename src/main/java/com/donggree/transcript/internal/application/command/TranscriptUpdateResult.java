package com.donggree.transcript.internal.application.command;

import com.donggree.transcript.internal.application.projection.TranscriptReportProjection.RawSemesterGroup;
import java.math.BigDecimal;
import java.util.List;

/**
 * 수강 이력 전체 치환(학업 정보 수정) 결과 반환 객체.
 * 치환 후 재계산된 총취득학점·평점 평균과, 학기 오름차순으로 그룹핑한 전체 수강 이력을 담는다.
 * 조회와 동일한 {@link RawSemesterGroup} 구조를 재사용해 컨트롤러 변환 로직을 공유한다.
 */
public record TranscriptUpdateResult(int totalCredits, BigDecimal gpa, List<RawSemesterGroup> semesterGroups) {}
