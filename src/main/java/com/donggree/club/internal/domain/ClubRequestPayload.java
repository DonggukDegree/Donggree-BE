package com.donggree.club.internal.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 요청의 변경 전·요청 값. 신규 등록·정보 수정은 동아리 정보와 사진 선택을, 모집 등록은 모집 정보를 담는다.
 * 사진 저장 전 요청에 있던 사진 수(imageCount) 같은 모르는 값은 읽을 때 무시한다.
 *
 * @param photos 동아리 정보 요청의 사진 선택. 모집 요청이면 null
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClubRequestPayload(ClubProfile profile, ClubPhotos photos, RecruitmentPlan recruitment) {

    public ClubRequestPayload {
        if ((profile == null) == (recruitment == null)) {
            throw new IllegalArgumentException("동아리 정보와 모집 정보 중 하나만 담습니다");
        }
        if (profile != null && photos == null) photos = ClubPhotos.NONE;
        if (recruitment != null) photos = null;
    }

    public static ClubRequestPayload of(ClubProfile profile, ClubPhotos photos) {
        return new ClubRequestPayload(profile, photos, null);
    }

    public static ClubRequestPayload of(RecruitmentPlan recruitment) {
        return new ClubRequestPayload(null, null, recruitment);
    }
}
