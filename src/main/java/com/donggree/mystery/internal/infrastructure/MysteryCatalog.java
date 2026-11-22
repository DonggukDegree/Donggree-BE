package com.donggree.mystery.internal.infrastructure;

import com.donggree.mystery.internal.domain.MysteryData.*;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.stereotype.Component;

/** 학교 공식 학과 홈페이지 목록·캠퍼스 안내에서 확인한 명칭. 게임 카탈로그는 커리큘럼과 분리한다. */
@Component
public class MysteryCatalog implements com.donggree.mystery.internal.application.MysteryCaseGenerator {
    private final SecureRandom random = new SecureRandom();
    // 학과: https://www.dongguk.edu/campus/list / 건물: 학교 2024_GUIDEBOOK 캠퍼스 지도.
    private static final List<Option> DEPARTMENTS = options(
            "D",
            "컴퓨터·AI학부",
            "경영학과",
            "경영정보학과",
            "회계학과",
            "경제학과",
            "법학과",
            "사학과",
            "철학과",
            "일본학과",
            "중어중문학과",
            "수학과",
            "통계학과",
            "화학과",
            "광고홍보학과",
            "영화영상학과");
    private static final List<Option> BUILDINGS = options(
            "B", "명진관", "혜화관", "학림관", "만해관", "법학관", "원흥관", "학생회관", "경영관", "사회과학관", "문화관", "학술관", "다향관", "중앙도서관", "상록원",
            "신공학관");
    private static final List<Option> METHODS = options(
            "M",
            "108배로 단련한 코끼리의 코",
            "학과사무실에서 빌린 가위",
            "상록원에서 빌린 집게",
            "남산 등산객에게 빌린 스틱",
            "팔정도에서 주운 나뭇가지",
            "수강신청으로 단련한 손놀림",
            "공강을 향한 간절한 염력",
            "과제 마감 직전에 발휘되는 괴력",
            "코끼리 귀로 일으킨 돌풍",
            "동아리방 구석의 잠자리채",
            "학생회실의 만능 테이프",
            "졸업생이 두고 간 학사모",
            "남산 바람을 담은 부채",
            "축제 부스에서 훔친 프라이팬",
            "과방 의자에 걸린 에코백");

    private static List<Option> options(String prefix, String... names) {
        List<Option> result = new ArrayList<>();
        for (int i = 0; i < names.length; i++) result.add(new Option(prefix + (i + 1), names[i]));
        return List.copyOf(result);
    }

    private List<Option> pick(List<Option> options) {
        var copy = new ArrayList<>(options);
        Collections.shuffle(copy, random);
        return List.copyOf(copy.subList(0, 6));
    }

    public CaseData generate() {
        var d = pick(DEPARTMENTS);
        var b = pick(BUILDINGS);
        var m = pick(METHODS);
        return new CaseData(
                d,
                b,
                m,
                new Guess(
                        d.get(random.nextInt(6)).id(),
                        b.get(random.nextInt(6)).id(),
                        m.get(random.nextInt(6)).id()));
    }
}
