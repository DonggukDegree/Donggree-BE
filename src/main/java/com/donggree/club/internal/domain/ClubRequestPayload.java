package com.donggree.club.internal.domain;

/**
 * 요청의 변경 전·요청 값. 신규 등록·정보 수정은 동아리 정보와 사진 수를, 모집 등록은 모집 정보를 담는다.
 *
 * @param imageCount 요청 화면에서 고른 사진 수(사진 저장 기능 전까지 비교 표시용)
 */
public record ClubRequestPayload(ClubProfile profile, Integer imageCount, RecruitmentPlan recruitment) {

    public static final int IMAGE_MAX_COUNT = 3;

    public ClubRequestPayload {
        if ((profile == null) == (recruitment == null)) {
            throw new IllegalArgumentException("동아리 정보와 모집 정보 중 하나만 담습니다");
        }
        if (profile != null && (imageCount == null || imageCount < 0 || imageCount > IMAGE_MAX_COUNT)) {
            throw new IllegalArgumentException("사진은 3장까지입니다");
        }
        if (recruitment != null) imageCount = null;
    }

    public static ClubRequestPayload of(ClubProfile profile, int imageCount) {
        return new ClubRequestPayload(profile, imageCount, null);
    }

    public static ClubRequestPayload of(RecruitmentPlan recruitment) {
        return new ClubRequestPayload(null, null, recruitment);
    }
}
