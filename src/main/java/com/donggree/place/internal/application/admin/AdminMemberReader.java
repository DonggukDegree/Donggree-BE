package com.donggree.place.internal.application.admin;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.transcript.TranscriptLookupService;
import com.donggree.user.MemberAdminProfile;
import com.donggree.user.UserModerationService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 회원 ID 목록을 신고 대시보드 회원 행으로 바꾼다. 회원·성적표·학과 정보를 한 번씩 묶어 조회한다. */
@Component
@RequiredArgsConstructor
public class AdminMemberReader {

    private final UserModerationService moderationService;
    private final TranscriptLookupService transcriptLookupService;
    private final CurriculumLookupService curriculumLookupService;

    public Map<Long, AdminMemberSummary> read(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, MemberAdminProfile> profiles = moderationService.findAdminProfiles(ids);
        return toSummaries(profiles.values());
    }

    public Map<Long, AdminMemberSummary> toSummaries(Collection<MemberAdminProfile> profiles) {
        List<Long> ids = profiles.stream().map(MemberAdminProfile::memberId).toList();
        Map<Long, Long> departmentIds =
                ids.isEmpty() ? Map.of() : transcriptLookupService.findDepartmentIdsByMemberIds(ids);
        List<Long> distinctDepartments =
                departmentIds.values().stream().distinct().toList();
        Map<Long, String> departmentNames = distinctDepartments.isEmpty()
                ? Map.of()
                : curriculumLookupService.findDepartmentNamesByIds(distinctDepartments);
        Map<Long, String> collegeNames = distinctDepartments.isEmpty()
                ? Map.of()
                : curriculumLookupService.findCollegeNamesByDepartmentIds(distinctDepartments);
        Map<Long, AdminMemberSummary> result = new HashMap<>();
        for (MemberAdminProfile profile : profiles) {
            Long departmentId = departmentIds.get(profile.memberId());
            result.put(
                    profile.memberId(),
                    new AdminMemberSummary(
                            profile.memberId(),
                            profile.nickname(),
                            profile.name(),
                            profile.studentId(),
                            departmentId == null ? null : collegeNames.get(departmentId),
                            departmentId == null ? null : departmentNames.get(departmentId),
                            profile.suspendedUntil()));
        }
        return result;
    }
}
