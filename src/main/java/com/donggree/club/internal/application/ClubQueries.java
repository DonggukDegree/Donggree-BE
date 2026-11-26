package com.donggree.club.internal.application;

import com.donggree.club.internal.application.readmodel.ClubListRow;
import java.util.List;

/** 동아리 읽기 모델을 반환하는 응용 계층 조회 계약. */
public interface ClubQueries {

    /** 모든 동아리의 목록 계산용 값과 가장 최근 모집 건(ID순). */
    List<ClubListRow> findListRows();
}
