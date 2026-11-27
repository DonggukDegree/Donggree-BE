package com.donggree.club.internal.presentation;

import com.donggree.club.internal.application.ClubImageService;
import com.donggree.club.internal.application.admin.AdminClubCommandService;
import com.donggree.club.internal.application.admin.AdminClubQueryService;
import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import com.donggree.club.internal.domain.enums.ClubRequestStatus;
import com.donggree.club.internal.presentation.dto.AdminClubRejectRequest;
import com.donggree.club.internal.presentation.dto.AdminClubRequestResponse;
import com.donggree.club.internal.presentation.dto.AdminClubResponse;
import com.donggree.club.internal.presentation.dto.ClubImageResponse;
import com.donggree.club.internal.presentation.dto.ClubSaveRequest;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 관리자 동아리 관리 API. `/api/admin/**`는 관리자만 호출할 수 있다. */
@RestController
@RequestMapping("/api/admin/clubs")
@RequiredArgsConstructor
public class AdminClubController {

    private final AdminClubQueryService queryService;
    private final AdminClubCommandService commandService;
    private final ClubImageService imageService;

    /** status를 비우면 전체. 검토 중은 먼저 들어온 순, 그 밖은 최신순이다. */
    @GetMapping("/requests")
    public ApiResponse<List<AdminClubRequestResponse>> getRequests(
            @RequestParam(required = false) ClubRequestStatus status) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                queryService.getRequests(status).stream()
                        .map(AdminClubRequestResponse::from)
                        .toList());
    }

    @PostMapping("/requests/{requestId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long requestId) {
        commandService.approve(requestId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long requestId, @RequestBody AdminClubRejectRequest request) {
        commandService.reject(requestId, request.reason());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    @GetMapping
    public ApiResponse<List<AdminClubResponse>> getClubs(@RequestParam(required = false) String keyword) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                queryService.getClubs(keyword).stream()
                        .map(AdminClubResponse::from)
                        .toList());
    }

    /** 임시 등록. 요청자 연락처·관계는 무시한다. */
    @PostMapping
    public ApiResponse<Long> registerProvisional(@RequestBody ClubSaveRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, commandService.registerProvisional(request.toCommand()));
    }

    @PutMapping("/{clubId}")
    public ApiResponse<Void> edit(@PathVariable Long clubId, @RequestBody ClubSaveRequest request) {
        commandService.edit(clubId, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }

    /** 관리자 사진 올리기. 배너 이미지는 purpose=BANNER(정리하지 않음), 그 밖은 동아리 사진이다. */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ClubImageResponse> uploadImage(
            @LoginMemberId Long adminId,
            @RequestParam(required = false) ClubImagePurpose purpose,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                ClubImageResponse.from(imageService.uploadByAdmin(adminId, ClubImageController.bytes(file), purpose)));
    }

    @DeleteMapping("/{clubId}/editors/{memberId}")
    public ApiResponse<Void> revokeEditor(@PathVariable Long clubId, @PathVariable Long memberId) {
        commandService.revokeEditor(clubId, memberId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
