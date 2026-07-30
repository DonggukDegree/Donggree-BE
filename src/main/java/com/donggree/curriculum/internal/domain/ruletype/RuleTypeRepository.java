package com.donggree.curriculum.internal.domain.ruletype;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleTypeRepository extends JpaRepository<RuleType, Long> {

    Optional<RuleType> findByTypeName(String typeName);
}
