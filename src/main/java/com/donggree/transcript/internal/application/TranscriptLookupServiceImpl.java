package com.donggree.transcript.internal.application;

import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.domain.TranscriptRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranscriptLookupServiceImpl implements TranscriptLookupService {
    private final TranscriptRepository transcriptRepository;
    private final TranscriptViewMapper viewMapper;

    @Override
    public Optional<TranscriptView> findById(Long transcriptId) {
        return transcriptRepository.findWithCourseRecordsById(transcriptId).map(viewMapper::toView);
    }

    @Override
    public Optional<TranscriptView> findByMemberId(Long memberId) {
        return transcriptRepository.findWithCourseRecordsByMemberId(memberId).map(viewMapper::toView);
    }
}
