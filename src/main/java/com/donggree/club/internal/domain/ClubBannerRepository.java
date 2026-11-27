package com.donggree.club.internal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClubBannerRepository extends JpaRepository<ClubBanner, Long> {

    /** 노출 순서대로. */
    List<ClubBanner> findAllByOrderBySortOrderAscIdAsc();

    @Query("select coalesce(max(b.sortOrder), 0) from ClubBanner b")
    int findMaxSortOrder();
}
