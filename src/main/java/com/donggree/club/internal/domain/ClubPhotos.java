package com.donggree.club.internal.domain;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 동아리 사진 선택(최대 3장, 순서대로). 대표 사진을 고르지 않았으면 첫 사진이 대표다.
 *
 * @param representativeImageId 사진이 없으면 null
 */
public record ClubPhotos(List<Long> imageIds, Long representativeImageId) {

    public static final int MAX_COUNT = 3;
    public static final ClubPhotos NONE = new ClubPhotos(List.of(), null);

    public ClubPhotos {
        imageIds = imageIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(imageIds));
        if (imageIds.size() > MAX_COUNT) {
            throw new IllegalArgumentException("사진은 3장까지입니다");
        }
        if (imageIds.isEmpty()) {
            representativeImageId = null;
        } else if (representativeImageId == null) {
            representativeImageId = imageIds.get(0);
        } else if (!imageIds.contains(representativeImageId)) {
            throw new IllegalArgumentException("대표 사진은 고른 사진 중 하나입니다");
        }
    }
}
