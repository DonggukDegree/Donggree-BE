package com.donggree.club.internal.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubImageRepository extends JpaRepository<ClubImage, Long> {

    Optional<ClubImage> findByStoredName(String storedName);

    /** 동아리에 연결된 사진(순서대로). */
    List<ClubImage> findByClubIdOrderBySortOrderAsc(Long clubId);

    /** 여러 동아리의 연결된 사진(동아리별 순서대로). */
    List<ClubImage> findByClubIdInOrderByClubIdAscSortOrderAsc(Collection<Long> clubIds);

    /**
     * 정리 대상: 동아리에 연결되지 않은 채 기준 시각 이전에 올렸고 검토 중 요청 값에도 없는 동아리 사진.
     * 요청 값(after_value)의 photos.imageIds 배열에 이 ID가 있으면 남긴다.
     */
    @Query(
            value = "SELECT i.* FROM club_image i WHERE i.club_id IS NULL AND i.purpose = 'CLUB' "
                    + "AND i.created_at < :before AND NOT EXISTS (SELECT 1 FROM club_request r "
                    + "WHERE r.status = 'PENDING' AND r.after_value -> 'photos' -> 'imageIds' @> to_jsonb(i.id))",
            nativeQuery = true)
    List<ClubImage> findExpiredUnattached(@Param("before") LocalDateTime before);
}
