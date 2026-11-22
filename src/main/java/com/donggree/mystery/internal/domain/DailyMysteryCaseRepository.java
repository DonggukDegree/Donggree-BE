package com.donggree.mystery.internal.domain;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DailyMysteryCaseRepository extends JpaRepository<DailyMysteryCase, LocalDate> {
    @Modifying
    @Query(
            value =
                    "INSERT INTO daily_mystery_case (game_date, case_data) VALUES (:date, CAST(:data AS jsonb)) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int createIfAbsent(@Param("date") LocalDate date, @Param("data") String data);
}
