package com.donggree.curriculum.internal.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaTypeRepository extends JpaRepository<AreaType, Long> {

    Optional<AreaType> findByAreaName(String areaName);
}
