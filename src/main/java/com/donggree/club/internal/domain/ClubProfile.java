package com.donggree.club.internal.domain;

import com.donggree.club.internal.domain.enums.ClubAffiliation;
import com.donggree.club.internal.domain.enums.ClubField;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 회원·관리자가 입력하는 동아리 정보. 생성 시 앞뒤 공백을 정리하고 입력 규칙을 검증한다.
 *
 * @param generation   현재 기수. null이면 기수 없이 활동(활동 기간도 비움)
 * @param roomLocation 동아리방 위치. null이면 동아리방 없음
 * @param foundedDate  창립일. 모르면 null
 */
public record ClubProfile(
        String name,
        ClubAffiliation affiliation,
        List<ClubField> fields,
        String description,
        Integer generation,
        LocalDate activityStartDate,
        LocalDate activityEndDate,
        String roomLocation,
        Integer generationMemberCount,
        Integer totalMemberCount,
        LocalDate foundedDate,
        List<ClubOfficer> officers,
        List<ClubLink> links) {

    public static final int NAME_MAX_LENGTH = 30;
    public static final int DESCRIPTION_MIN_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;
    public static final int FIELDS_MAX_COUNT = 3;
    public static final int ROOM_MAX_LENGTH = 50;

    public ClubProfile {
        name = name == null ? "" : name.strip();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("동아리 이름은 1~30자입니다");
        }
        if (affiliation == null) {
            throw new IllegalArgumentException("소속 분류를 골라야 합니다");
        }
        fields = fields == null ? List.of() : List.copyOf(new LinkedHashSet<>(fields));
        if (fields.isEmpty() || fields.size() > FIELDS_MAX_COUNT) {
            throw new IllegalArgumentException("활동 분야는 1~3개입니다");
        }
        description = description == null ? "" : description.strip();
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("동아리 소개는 100~1000자입니다");
        }
        if (generation == null) {
            if (activityStartDate != null || activityEndDate != null) {
                throw new IllegalArgumentException("기수 없이 활동하면 활동 기간을 두지 않습니다");
            }
        } else if (generation < 1
                || activityStartDate == null
                || activityEndDate == null
                || activityStartDate.isAfter(activityEndDate)) {
            throw new IllegalArgumentException("현재 기수와 활동 기간을 확인해주세요");
        }
        if (roomLocation != null) {
            roomLocation = roomLocation.strip();
            if (roomLocation.isEmpty() || roomLocation.length() > ROOM_MAX_LENGTH) {
                throw new IllegalArgumentException("동아리방 위치는 1~50자입니다");
            }
        }
        if (!isPositiveOrNull(generationMemberCount) || !isPositiveOrNull(totalMemberCount)) {
            throw new IllegalArgumentException("인원은 1명 이상입니다");
        }
        officers = officers == null ? List.of() : List.copyOf(officers);
        if (officers.size() > ClubOfficer.MAX_COUNT) {
            throw new IllegalArgumentException("운영진은 5명까지입니다");
        }
        links = links == null ? List.of() : List.copyOf(links);
        if (links.size() > ClubLink.MAX_COUNT) {
            throw new IllegalArgumentException("관련 링크는 3개까지입니다");
        }
    }

    private static boolean isPositiveOrNull(Integer value) {
        return value == null || value >= 1;
    }
}
