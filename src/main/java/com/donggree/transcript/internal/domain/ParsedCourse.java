package com.donggree.transcript.internal.domain;

/**
 * PDF에서 파싱한 개별 교과목 정보를 담는 레코드.
 * DB 엔티티로 변환되기 전의 원시 파싱 결과이다.
 *
 * @param semester   이수 학기 (예: "2023-1", "2024-여름")
 * @param year       학년 (1~6)
 * @param category   이수구분 원시값 (공교, 전공, 전필, 일교, 학기, 자선)
 * @param courseCode 과목 코드 (예: "CSE1101", "123456")
 * @param courseName 과목명
 * @param credits    학점
 * @param grade      성적 원시값 (예: "A+", "B0", "P", "NP")
 * @param area       영역 원시값 (예: "자아", "기초", "1"). 해당 없으면 빈 문자열
 * @param retake     재수강 여부
 */
public record ParsedCourse(
        String semester,
        int year,
        String category,
        String courseCode,
        String courseName,
        int credits,
        String grade,
        String area,
        boolean retake) {}
