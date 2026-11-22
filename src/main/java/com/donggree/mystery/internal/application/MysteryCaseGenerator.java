package com.donggree.mystery.internal.application;

import com.donggree.mystery.internal.domain.MysteryData.CaseData;

/** 출제 카탈로그와 난수 인프라에 대한 응용 계층 포트. */
public interface MysteryCaseGenerator {
    CaseData generate();
}
