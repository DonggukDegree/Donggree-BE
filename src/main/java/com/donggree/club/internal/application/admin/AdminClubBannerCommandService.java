package com.donggree.club.internal.application.admin;

import com.donggree.club.internal.application.command.BannerSaveCommand;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.domain.BannerContent;
import com.donggree.club.internal.domain.ClubBanner;
import com.donggree.club.internal.domain.ClubBannerRepository;
import com.donggree.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 배너 등록·수정·삭제·노출 순서 변경. */
@Service
@RequiredArgsConstructor
public class AdminClubBannerCommandService {

    private final ClubBannerRepository bannerRepository;

    /** 맨 뒤 순서로 등록한다. */
    @Transactional
    public Long create(BannerSaveCommand command) {
        return bannerRepository
                .save(ClubBanner.create(content(command), bannerRepository.findMaxSortOrder() + 1))
                .getId();
    }

    @Transactional
    public void revise(Long bannerId, BannerSaveCommand command) {
        find(bannerId).revise(content(command));
    }

    @Transactional
    public void delete(Long bannerId) {
        bannerRepository.delete(find(bannerId));
    }

    /** 노출 순서를 한 칸 앞(-1)·뒤(+1)로 옮긴다. 이미 맨 앞·맨 뒤면 그대로 둔다. */
    @Transactional
    public void move(Long bannerId, int offset) {
        if (offset != -1 && offset != 1) throw new GeneralException(ClubErrorCode.INVALID_BANNER);
        List<ClubBanner> ordered = bannerRepository.findAllByOrderBySortOrderAscIdAsc();
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(bannerId)) index = i;
        }
        if (index < 0) throw new GeneralException(ClubErrorCode.BANNER_NOT_FOUND);
        int target = index + offset;
        if (target < 0 || target >= ordered.size()) return;
        ordered.get(index).swapOrder(ordered.get(target));
    }

    private ClubBanner find(Long bannerId) {
        return bannerRepository
                .findById(bannerId)
                .orElseThrow(() -> new GeneralException(ClubErrorCode.BANNER_NOT_FOUND));
    }

    private static BannerContent content(BannerSaveCommand command) {
        try {
            return new BannerContent(
                    command.title(),
                    command.eyebrow(),
                    command.description(),
                    command.background(),
                    command.imageUrl(),
                    command.mobileImageUrl(),
                    command.link(),
                    command.startsAt(),
                    command.endsAt(),
                    command.active());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_BANNER);
        }
    }
}
