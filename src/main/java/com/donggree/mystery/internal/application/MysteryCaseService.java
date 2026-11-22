package com.donggree.mystery.internal.application;

import com.donggree.mystery.internal.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ON CONFLICT를 사용해 다중 인스턴스·동시 첫 접속에서도 최초 문제를 보존한다. */
@Service
@RequiredArgsConstructor
public class MysteryCaseService {
    private final DailyMysteryCaseRepository repository;
    private final MysteryCaseGenerator catalog;
    private final ObjectMapper mapper;

    @Transactional
    public DailyMysteryCase getOrCreate(LocalDate date) {
        var existing = repository.findById(date);
        if (existing.isPresent()) return existing.get();
        try {
            repository.createIfAbsent(date, mapper.writeValueAsString(catalog.generate()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("출제 스냅샷 직렬화 실패", e);
        }
        return repository.findById(date).orElseThrow();
    }
}
