package com.donggree.transcript.internal.presentation;

import com.donggree.curriculum.CurriculumLookupService;
import com.donggree.user.MemberIdentityService;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.transcript.internal.application.CourseRecordCreateData;
import com.donggree.transcript.internal.application.TranscriptParseResult;
import com.donggree.transcript.internal.application.TranscriptQueryResult;
import com.donggree.transcript.internal.application.TranscriptQueryResult.RawSemesterGroup;
import com.donggree.transcript.internal.application.TranscriptService;
import com.donggree.transcript.internal.application.exception.TranscriptErrorCode;
import com.donggree.transcript.internal.domain.ParsedTranscriptData;
import com.donggree.transcript.internal.domain.TranscriptCreateData;
import com.donggree.transcript.internal.domain.enums.Grade;
import com.donggree.transcript.internal.presentation.dto.CourseRecordAddRequest;
import com.donggree.transcript.internal.presentation.dto.CourseRecordAddResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptCreateResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse;
import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse.CourseRecord;
import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse.Meta;
import com.donggree.transcript.internal.presentation.dto.TranscriptReportResponse.SemesterCourses;
import com.donggree.transcript.internal.presentation.swagger.TranscriptApi;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/reports")
@RequiredArgsConstructor
public class TranscriptController implements TranscriptApi {

    private final TranscriptService transcriptService;
    private final CurriculumLookupService curriculumLookupService;
    private final MemberIdentityService memberIdentityService;

    @Override
    @GetMapping
    public ApiResponse<TranscriptReportResponse> getTranscriptReport(
            @LoginMemberId Long memberId
    ) {
        TranscriptQueryResult raw = transcriptService.getTranscriptRawReport(memberId);

        List<Long> deptIds = Stream.of(
                        raw.meta().departmentId(), raw.meta().subMajor1Id(),
                        raw.meta().subMajor2Id(), raw.meta().dualMajor1Id(), raw.meta().dualMajor2Id())
                .filter(Objects::nonNull).distinct().toList();

        Map<Long, String> deptNameMap = curriculumLookupService.findDepartmentNamesByIds(deptIds);

        Meta meta = new Meta(
                raw.meta().reportId(),
                raw.meta().admissionYear(),
                deptName(deptNameMap, raw.meta().departmentId()),
                deptName(deptNameMap, raw.meta().subMajor1Id()),
                deptName(deptNameMap, raw.meta().subMajor2Id()),
                deptName(deptNameMap, raw.meta().dualMajor1Id()),
                deptName(deptNameMap, raw.meta().dualMajor2Id()),
                raw.meta().academicStatus(),
                raw.meta().totalCredits(),
                raw.meta().gpa(),
                raw.meta().completedSemesters()
        );

        List<SemesterCourses> courses = raw.semesterGroups().stream()
                .map(g -> new SemesterCourses(g.semester(), toCourseRecords(g)))
                .toList();

        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                new TranscriptReportResponse(meta, courses));
    }

    @Override
    @PutMapping
    public ApiResponse<TranscriptCreateResponse> createTranscript(
            @LoginMemberId Long memberId,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(TranscriptErrorCode.PDF_FILE_REQUIRED);
        }
        try {
            TranscriptParseResult parseResult = transcriptService.parseTranscript(file.getBytes());
            ParsedTranscriptData parsed = parseResult.parsedData();
            Map<String, String> meta = parsed.meta();

            String pdfStudentId = meta.get("학번");
            String pdfName = meta.get("성명");
            if (pdfStudentId == null || pdfName == null
                    || meta.get("학적상태") == null || meta.get("교육과정 적용년도") == null
                    || meta.get("총취득학점") == null || meta.get("평점평균") == null
                    || meta.get("이수학기") == null) {
                throw new GeneralException(TranscriptErrorCode.PDF_PARSING_FAILED);
            }
            memberIdentityService.validatePdfOwner(memberId, pdfStudentId, pdfName);

            Long deptId = resolveDepartmentId(meta.get("학과"));
            Long sub1Id = curriculumLookupService.findDepartmentIdByName(meta.get("부전공1")).orElse(null);
            Long sub2Id = curriculumLookupService.findDepartmentIdByName(meta.get("부전공2")).orElse(null);
            Long dual1Id = curriculumLookupService.findDepartmentIdByName(meta.get("복수1")).orElse(null);
            Long dual2Id = curriculumLookupService.findDepartmentIdByName(meta.get("복수2")).orElse(null);

            TranscriptCreateData createData = transcriptService.buildCreateData(
                    memberId, parseResult.rawDataJson(), parsed, deptId, sub1Id, sub2Id, dual1Id, dual2Id);

            List<CourseRecordCreateData> courses = parsed.courses().stream()
                    .map(c -> new CourseRecordCreateData(
                            c.semester(),
                            c.category(),
                            (c.area() == null || c.area().isBlank()) ? null : c.area(),
                            c.courseCode(),
                            c.courseName(),
                            c.credits(),
                            Grade.fromValue(c.grade()),
                            c.retake()
                    ))
                    .toList();

            Long reportId = transcriptService.createTranscript(createData, courses, pdfStudentId, pdfName);
            return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, new TranscriptCreateResponse(reportId));

        } catch (IOException e) {
            throw new GeneralException(TranscriptErrorCode.INVALID_PDF_FILE);
        }
    }

    @Override
    @PatchMapping
    public ApiResponse<CourseRecordAddResponse> addCourseRecords(
            @LoginMemberId Long memberId,
            @Valid @RequestBody CourseRecordAddRequest request
    ) {
        List<CourseRecordCreateData> courses = request.courses().stream()
                .map(item -> {
                    try {
                        return new CourseRecordCreateData(
                                item.semester(),
                                item.courseType(),
                                (item.areaName() == null || item.areaName().isBlank()) ? null : item.areaName(),
                                item.courseCode(),
                                item.courseName(),
                                item.credits(),
                                Grade.fromValue(item.grade()),
                                item.retake()
                        );
                    } catch (IllegalArgumentException e) {
                        throw new GeneralException(TranscriptErrorCode.INVALID_COURSE_DATA);
                    }
                })
                .toList();

        List<Long> addedIds = transcriptService.addCourseRecords(memberId, courses);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, new CourseRecordAddResponse(addedIds));
    }

    private Long resolveDepartmentId(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) return null;
        return curriculumLookupService.findDepartmentIdByName(departmentName)
                .orElseThrow(() -> new GeneralException(TranscriptErrorCode.DEPARTMENT_NOT_FOUND));
    }

    private String deptName(Map<Long, String> map, Long id) {
        return id != null ? map.get(id) : null;
    }

    private List<CourseRecord> toCourseRecords(RawSemesterGroup group) {
        return group.records().stream()
                .map(r -> new CourseRecord(
                        r.id(),
                        r.courseCode(),
                        r.courseName(),
                        r.credits(),
                        r.courseType(),
                        r.areaName(),
                        r.grade(),
                        r.retake()
                ))
                .toList();
    }
}
