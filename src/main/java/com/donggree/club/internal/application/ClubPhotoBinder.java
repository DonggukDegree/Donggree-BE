package com.donggree.club.internal.application;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.ClubImage;
import com.donggree.club.internal.domain.ClubImageRepository;
import com.donggree.club.internal.domain.ClubPhotos;
import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 동아리 사진 선택을 검증하고 동아리에 연결한다. 회원은 직접 올린(아직 연결 안 된) 사진이나 이미 그 동아리 사진만 쓸 수 있다.
 * 새 선택에서 빠진 사진은 연결을 끊고, 파일은 정리 작업이 지운다.
 */
@Component
@RequiredArgsConstructor
public class ClubPhotoBinder {

    private final ClubImageRepository imageRepository;
    private final ClubImageUrls imageUrls;

    /** @param clubId 정보 수정이면 대상 동아리, 신규 등록이면 null */
    public void requireUsable(Long memberId, Long clubId, ClubPhotos photos) {
        List<ClubImage> images = imageRepository.findAllById(photos.imageIds());
        if (images.size() != photos.imageIds().size()
                || images.stream().anyMatch(image -> !image.usableBy(memberId, clubId))) {
            throw new GeneralException(ClubErrorCode.INVALID_PHOTOS);
        }
    }

    /** 관리자는 연결 안 된 동아리 사진이나 이 동아리 사진을 쓸 수 있다. */
    public void requireUsableByAdmin(Long clubId, ClubPhotos photos) {
        List<ClubImage> images = imageRepository.findAllById(photos.imageIds());
        if (images.size() != photos.imageIds().size()
                || images.stream()
                        .anyMatch(image -> image.getPurpose() != ClubImagePurpose.CLUB
                                || (image.getClubId() != null
                                        && !image.getClubId().equals(clubId)))) {
            throw new GeneralException(ClubErrorCode.INVALID_PHOTOS);
        }
    }

    /** 동아리 사진을 선택대로 맞춘다. 승인 시점에 이미 없어진 사진은 건너뛴다. */
    public void bind(Long clubId, ClubPhotos photos) {
        Map<Long, ClubImage> selected = imageRepository.findAllById(photos.imageIds()).stream()
                .collect(Collectors.toMap(ClubImage::getId, Function.identity()));
        imageRepository.findByClubIdOrderBySortOrderAsc(clubId).stream()
                .filter(image -> !selected.containsKey(image.getId()))
                .forEach(ClubImage::detach);
        int order = 0;
        for (Long imageId : photos.imageIds()) {
            ClubImage image = selected.get(imageId);
            if (image == null) continue;
            image.attach(clubId, order++, imageId.equals(photos.representativeImageId()));
        }
    }

    /** 지금 동아리 사진 선택(요청 비교용 변경 전 값). */
    public ClubPhotos current(Long clubId) {
        List<ClubImage> images = imageRepository.findByClubIdOrderBySortOrderAsc(clubId);
        return new ClubPhotos(
                images.stream().map(ClubImage::getId).toList(),
                images.stream()
                        .filter(ClubImage::isRepresentative)
                        .map(ClubImage::getId)
                        .findFirst()
                        .orElse(null));
    }

    /** 사진 ID별 주소. 없어진 사진은 넣지 않는다. */
    public Map<Long, String> urls(Collection<Long> imageIds) {
        if (imageIds.isEmpty()) return Map.of();
        return imageRepository.findAllById(imageIds).stream()
                .collect(Collectors.toMap(ClubImage::getId, imageUrls::of));
    }
}
