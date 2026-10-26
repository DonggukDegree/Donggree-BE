package com.donggree.place.internal.domain;

import com.donggree.place.internal.domain.enums.PlaceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByIdAndStatus(Long id, PlaceStatus status);

    /** 중복 검사 후보를 사각 범위로 좁혀 조회한다. 정확한 반경 판정은 도메인에서 한다. */
    List<Place> findByStatusAndLatitudeBetweenAndLongitudeBetween(
            PlaceStatus status, double minLatitude, double maxLatitude, double minLongitude, double maxLongitude);

    /**
     * 탈퇴 회원의 등록자 연결을 끊는다. 재가입 시 같은 회원 ID가 재활성화되므로 ID를 남기지 않는다.
     * 벌크 갱신이라 버전·수정 시각을 바꾸지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Place p set p.createdBy = null where p.createdBy = :memberId")
    int clearCreatedBy(@Param("memberId") Long memberId);
}
