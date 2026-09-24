package com.donggree.transcript.internal.domain;

import java.util.List;
import java.util.Map;

/**
 * PDF에서 파싱한 전체 성적표 정보를 담는 레코드.
 * 메타 정보(학번, 학과, GPA 등)와 개별 교과목 목록을 포함한다.
 *
 * <p>메타 정보 주요 키:
 * <ul>
 *   <li>교육과정 적용년도, 과정, 학과, 학번, 성명</li>
 *   <li>학적상태, 학년, 총취득학점, 평점평균, 이수학기</li>
 *   <li>부전공1, 부전공2, 복수1, 복수2</li>
 *   <li>공학인증심화대상, 전적대, 글로벌인재트랙여부, 선택적수료승인</li>
 *   <li>영어강의이수결과, 영어패스제결과, 졸업논문심사, 교직인적성합격횟수</li>
 * </ul>
 *
 * @param meta    메타 정보 키-값 맵. 값이 없는 항목은 null
 * @param courses 파싱된 교과목 목록
 */
public record ParsedTranscriptData(Map<String, String> meta, List<ParsedCourse> courses) {
    /** 학번이나 복수전공 신청 학기가 아닌 PDF 교육과정 적용년도를 사용한다. */
    public int admissionYear() {
        String value = meta.get("교육과정 적용년도");
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public boolean engineeringCertified() {
        String value = meta.get("공학인증심화대상");
        return value != null && !value.isBlank() && !"N".equals(value.trim());
    }
}
