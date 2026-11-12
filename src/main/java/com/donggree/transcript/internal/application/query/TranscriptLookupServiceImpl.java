package com.donggree.transcript.internal.application.query;

import com.donggree.transcript.TranscriptLookupService;
import com.donggree.transcript.TranscriptView;
import com.donggree.transcript.internal.domain.transcript.TranscriptRepository;
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

    @Override
    public java.util.Map<Long, Long> findDepartmentIdsByMemberIds(java.util.Collection<Long> memberIds) {
        var ids =
                memberIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }
        return transcriptRepository.findDepartmentIdsByMemberIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0], row -> (Long) row[1], (first, second) -> first));
    }
}
