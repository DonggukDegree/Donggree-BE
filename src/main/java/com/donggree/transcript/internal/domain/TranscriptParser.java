package com.donggree.transcript.internal.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF에서 추출한 텍스트를 파싱하여 성적 메타 정보와 수강 이력을 추출하는 도메인 서비스.
 * nDRIMS "취득교과목 영역별 분류표" PDF 형식에 특화되어 있다.
 */
public class TranscriptParser {

    /** 이수구분별 유효 영역 매핑 */
    private static final Map<String, Set<String>> AREA_BY_CATEGORY = Map.of(
            "공교", Set.of("자아", "동국", "시민", "대학", "리더", "명작", "사고", "미래", "창의", "자기", "영어", "글", "SW", "한국"),
            "전공", Set.of("기초", "전문"),
            "전필", Set.of("기초", "전문"),
            "복수1", Set.of("기초", "전문"),
            "복수2", Set.of("기초", "전문"),
            "일교", Set.of("1", "2", "3", "4", "5", "6"),
            "학기", Set.of("1", "2", "3", "4", "5", "6"),
            "자선", Set.of("기초", "전문"));

    // ====== 정규표현식 구성 요소 ======

    private static final String GRADE_RE = "(?:A[+0]|B[+0]|C[+0]|D[+0]|[A-DF]|NP|P)";
    private static final String AREA_RE = "(?:[가-힣]{1,2}|[A-Za-z]{2}|\\d)";
    private static final String SEMESTER_RE = "\\d{4}-(?:[12]|여름|겨울|공통)";
    private static final String CATEGORY_RE = "공교|전공|전필|복수1|복수2|일교|학기|자선";
    private static final String CODE_RE = "[A-Z]{2,3}\\d{3,5}|\\d{6}";

    /** 과목 행 시작 패턴: 학기 학년 이수구분 과목코드 */
    private static final Pattern COURSE_START =
            Pattern.compile("(" + SEMESTER_RE + ")\\s+(\\d)\\s+(" + CATEGORY_RE + ")\\s+(" + CODE_RE + ")\\s+");

    /** 과목 행 꼬리 패턴: 학점 성적 [영역] [재수강] */
    private static final Pattern TAIL =
            Pattern.compile("\\s+(\\d{1,2})\\s+(" + GRADE_RE + ")(?:\\s+(" + AREA_RE + "))?(?:\\s+(R))?");

    // ====== 메타 파싱용 키 목록 ======

    /** PDF 상단 메타 영역에 등장하는 키 (정규표현식 이스케이프 포함) */
    private static final String[] META_KEYS = {
        "교육과정 적용년도",
        "과정",
        "공학인증심화대상",
        "레벨테스트\\(텝스\\)",
        "레벨테스트\\(인터뷰\\)",
        "글로벌인재트랙여부",
        "대학",
        "특기",
        "학적상태",
        "전적대",
        "전과\\(학과\\)",
        "학과",
        "학번",
        "성명",
        "부전공1",
        "부전공2",
        "복수1",
        "복수2",
        "캠퍼스전입여부",
        "선택적수료승인",
    };

    /** PDF 하단 요약 영역의 파싱 규칙: [키 이름, 정규표현식] */
    private static final String[][] SUMMARY_RULES = {
        {"총취득학점", "총취득학점\\s*:?\\s*(\\d+)"},
        {"평점평균", "평점평균\\s*:?\\s*([\\d.]+)"},
        {"제1전공총학점", "제1전공\\s*:?\\s*총\\s*(\\d+)\\s*학점"},
        {"제1전공평점", "제1전공평점\\s*:?\\s*([\\d.]+)"},
        {"공통교양총학점", "공통교양\\s*:?\\s*총\\s*(\\d+)\\s*학점"},
        {"교양선택총학점", "교양선택\\s*:?\\s*총\\s*(\\d+)\\s*학점"},
        {"자유선택총학점", "자선\\s*:?\\s*총\\s*(\\d+)\\s*학점"},
        {"영어강의이수대상", "영어강의이수\\s*:?\\s*(대상)"},
        {"영어강의이수결과", "영어강의이수\\s*:?.*?(PASS|FAIL)"},
        {"영어패스제결과", "영어패스제.*?(PASS|FAIL)"},
        {"졸업논문심사", "졸업논문.*?심사.*?:\\s*([가-힣]+)"},
        {"교직인적성합격횟수", "교직인적성합격횟수\\s*:\\s*(\\d+)"},
        {"주전공", "주전공\\s*:?\\s*(.+?)\\s+DC"},
        {"주전공코드", "(DC-\\d+(?:\\([^)]*\\))?)"},
        {"복수1총학점", "복수1\\s*:\\s*총\\s*(\\d+)\\s*학점"},
        {"복수1기초학점", "복수1\\s*:\\s*총\\s*\\d+\\s*학점\\s*\\([^)]*?기초\\s*:\\s*(\\d+)"},
        {"복수1전문학점", "복수1\\s*:\\s*총\\s*\\d+\\s*학점\\s*\\([^)]*?전문\\s*:\\s*(\\d+)"},
        {"복수1평점", "복수1평점\\s*:\\s*([\\d.]+)"},
        {"복수1졸업논문심사", "졸업논문\\(시험\\)\\s*심사\\(복수1\\)\\s*:\\s*([가-힣]+)"},
        {"복수2총학점", "복수2\\s*:\\s*총\\s*(\\d+)\\s*학점"},
        {"복수2기초학점", "복수2\\s*:\\s*총\\s*\\d+\\s*학점\\s*\\([^)]*?기초\\s*:\\s*(\\d+)"},
        {"복수2전문학점", "복수2\\s*:\\s*총\\s*\\d+\\s*학점\\s*\\([^)]*?전문\\s*:\\s*(\\d+)"},
        {"복수2평점", "복수2평점\\s*:\\s*([\\d.]+)"},
        {"복수2졸업논문심사", "졸업논문\\(시험\\)\\s*심사\\(복수2\\)\\s*:\\s*([가-힣]+)"},
    };

    /** 복수전공 학과명 뒤에 붙는 기준 학기 표기. 예: 전자전기공학부(2026-2) */
    private static final Pattern DUAL_MAJOR_WITH_REFERENCE_SEMESTER =
            Pattern.compile("(.+?)\\s*\\((\\d{4}-(?:[12]|여름|겨울))\\)\\s*");

    /**
     * PDF에서 추출한 텍스트를 파싱하여 메타 정보와 수강 이력을 반환한다.
     *
     * @param pdfText PDFBox 등으로 추출한 원시 텍스트
     * @return 파싱된 성적표 데이터 (메타 정보 + 교과목 목록)
     * @throws IllegalArgumentException pdfText가 null인 경우
     */
    public ParsedTranscriptData parse(String pdfText) {
        if (pdfText == null) {
            throw new IllegalArgumentException("pdfText must not be null");
        }
        Map<String, String> meta = parseMeta(pdfText);
        List<ParsedCourse> courses = parseCourses(pdfText);
        return new ParsedTranscriptData(meta, courses);
    }

    // ====== 메타 파싱 (키 경계 방식) ======

    Map<String, String> parseMeta(String text) {
        Map<String, String> meta = new LinkedHashMap<>();

        // 1단계: 상단 메타 — 키 경계 방식으로 값 추출
        String keyPattern = "(" + String.join("|", META_KEYS) + ")\\s*:";
        List<int[]> keyPositions = new ArrayList<>();
        List<String> keyNames = new ArrayList<>();

        Matcher km = Pattern.compile(keyPattern).matcher(text);
        while (km.find()) {
            keyPositions.add(new int[] {km.start(), km.end()});
            keyNames.add(km.group(1));
        }

        Set<String> parsedTopKeys = new HashSet<>();
        for (int i = 0; i < keyPositions.size(); i++) {
            String key = keyNames.get(i);
            // "복수1"처럼 상단 학생 정보와 하단 학점 요약에 같은 이름이 쓰이는 키가 있다.
            // 상단 메타는 최초 출현값만 채택하고 하단 값은 SUMMARY_RULES의 별도 키로 파싱한다.
            if (!parsedTopKeys.add(key)) {
                continue;
            }
            int valStart = keyPositions.get(i)[1];
            int valEnd = (i + 1 < keyPositions.size()) ? keyPositions.get(i + 1)[0] : valStart + 100;
            valEnd = Math.min(valEnd, text.length());

            String value = text.substring(valStart, valEnd).trim();
            int newline = value.indexOf('\n');
            if (newline >= 0) {
                value = value.substring(0, newline).trim();
            }
            meta.put(key, value.isEmpty() ? null : value);
        }

        // 학과에서 "N학년" 분리: "컴퓨터·AI학부 4학년" → 학과=컴퓨터·AI학부, 학년=4
        String dept = meta.getOrDefault("학과", "");
        if (dept != null) {
            Matcher ym = Pattern.compile("(.+?)\\s+(\\d)학년").matcher(dept);
            if (ym.matches()) {
                meta.put("학과", ym.group(1).trim());
                meta.put("학년", ym.group(2));
            }
        }

        // 복수전공은 "전자전기공학부(2026-2)"처럼 학과명 뒤에 기준 학기가 붙는다.
        // 학과 ID 조회에는 정제된 학과명만 사용하고, 기준 학기는 raw_data.meta에 별도로 보존한다.
        normalizeDualMajor(meta, "복수1");
        normalizeDualMajor(meta, "복수2");

        // 2단계: 하단 요약 — 개별 정규표현식으로 파싱
        for (String[] rule : SUMMARY_RULES) {
            Matcher m = Pattern.compile(rule[1]).matcher(text);
            meta.put(rule[0], m.find() ? m.group(1).trim() : null);
        }

        // 3단계: 이수학기 계산 — <<등록사항>>에서 등필 정규학기 수 (여름/겨울 제외)
        Matcher sem =
                Pattern.compile("(\\d{4})\\s*-\\s*([12여름겨울]+)\\s+\\d학년\\s+등필").matcher(text);
        int regularSemesters = 0;
        while (sem.find()) {
            String term = sem.group(2);
            if ("1".equals(term) || "2".equals(term)) {
                regularSemesters++;
            }
        }
        meta.put("이수학기", regularSemesters > 0 ? String.valueOf(regularSemesters) : null);

        return meta;
    }

    private void normalizeDualMajor(Map<String, String> meta, String key) {
        String value = meta.get(key);
        String referenceSemesterKey = key + "기준학기";
        if (value == null || value.isBlank()) {
            meta.put(referenceSemesterKey, null);
            return;
        }

        Matcher matcher = DUAL_MAJOR_WITH_REFERENCE_SEMESTER.matcher(value);
        if (!matcher.matches()) {
            meta.put(referenceSemesterKey, null);
            return;
        }

        meta.put(key, matcher.group(1).trim());
        meta.put(referenceSemesterKey, matcher.group(2));
    }

    // ====== 과목 파싱 ======

    List<ParsedCourse> parseCourses(String text) {
        text = preprocessText(text);
        List<ParsedCourse> courses = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();
        List<String[]> groups = new ArrayList<>();

        Matcher sm = COURSE_START.matcher(text);
        while (sm.find()) {
            positions.add(new int[] {sm.start(), sm.end()});
            groups.add(new String[] {sm.group(1), sm.group(2), sm.group(3), sm.group(4)});
        }

        for (int i = 0; i < positions.size(); i++) {
            int nameStart = positions.get(i)[1];
            int searchEnd =
                    (i + 1 < positions.size()) ? positions.get(i + 1)[0] : Math.min(nameStart + 200, text.length());
            String region = text.substring(nameStart, searchEnd);

            Matcher tm = TAIL.matcher(region);
            if (!tm.find()) {
                continue;
            }

            String semester = groups.get(i)[0];
            int year = Integer.parseInt(groups.get(i)[1]);
            String category = groups.get(i)[2];
            String courseCode = groups.get(i)[3];
            String courseName = region.substring(0, tm.start()).trim();
            int credits = Integer.parseInt(tm.group(1));
            String grade = tm.group(2);
            boolean retake = "R".equals(tm.group(4));

            String rawArea = tm.group(3) != null ? tm.group(3).trim() : "";
            String area = AREA_BY_CATEGORY.getOrDefault(category, Set.of()).contains(rawArea) ? rawArea : "";

            courses.add(
                    new ParsedCourse(semester, year, category, courseCode, courseName, credits, grade, area, retake));
        }

        return courses;
    }

    /**
     * PDF 추출 텍스트의 줄바꿈 깨짐을 보정한다.
     * PDFBox가 과목 행을 여러 줄로 분리하는 경우를 하나로 합친다.
     */
    private String preprocessText(String text) {
        // 학기+학년 / 이수구분 / 과목코드가 줄바꿈으로 분리된 경우
        text = text.replaceAll(
                "(" + SEMESTER_RE + "\\s+\\d)\\s*\\n\\s*(" + CATEGORY_RE + ")\\s*\\n\\s*(" + CODE_RE + ")", "$1 $2 $3");
        // 과목코드 / 과목명이 줄바꿈으로 분리된 경우
        text = text.replaceAll("(" + CODE_RE + ")\\s*\\n\\s*([가-힣<])", "$1 $2");
        // 과목명 / 학점 / 성적이 줄바꿈으로 분리된 경우
        text = text.replaceAll("([)가-힣\\w])\\s*\\n\\s*(\\d)\\s*\\n\\s*(" + GRADE_RE + ")", "$1 $2 $3");
        return text;
    }
}
