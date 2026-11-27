package com.donggree.club.internal.application.readmodel;

import com.donggree.club.internal.domain.ClubRequest;
import com.donggree.club.internal.domain.ClubRequestPayload;
import com.donggree.club.internal.domain.enums.ClubRequesterRole;
import java.util.Map;

/**
 * 내 요청 한 건과 보냈던 값(수정해서 다시 요청할 때 불러온다).
 *
 * @param requesterContact 보냈던 확인 연락처. 보유 기간이 지나 비웠으면 null
 * @param requesterRole    보냈던 동아리와의 관계. 보유 기간이 지나 비웠으면 null
 * @param imageUrls        보냈던 사진 ID별 주소. 정리돼 없어진 사진은 빠진다
 */
public record MyClubRequestDetailReadModel(
        MyClubRequestReadModel request,
        ClubRequestPayload after,
        String requesterContact,
        ClubRequesterRole requesterRole,
        Map<Long, String> imageUrls) {

    public static MyClubRequestDetailReadModel from(ClubRequest request, Map<Long, String> imageUrls) {
        return new MyClubRequestDetailReadModel(
                MyClubRequestReadModel.from(request),
                request.getAfter(),
                request.getRequesterContact(),
                request.getRequesterRole(),
                imageUrls);
    }
}
