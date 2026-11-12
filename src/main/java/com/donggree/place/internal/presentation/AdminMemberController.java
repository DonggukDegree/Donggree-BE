package com.donggree.place.internal.presentation;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.auth.LoginMemberId;
import com.donggree.place.internal.application.admin.AdminMemberService;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberRow;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.presentation.dto.AdminSuspensionRequest;
import com.donggree.place.internal.presentation.swagger.AdminMemberApi;
import com.donggree.user.MemberSuspensionRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 신고 대시보드의 회원 관리 API. 회원 검색·상세와 신고와 별개인 관리자 정지·해제. */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController implements AdminMemberApi {

    private final AdminMemberService memberService;

    @Override
    @GetMapping
    public ApiResponse<Page<MemberRow>> searchMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean suspended,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, memberService.searchMembers(keyword, suspended, page));
    }

    @Override
    @GetMapping("/{memberId}")
    public ApiResponse<MemberDetail> getMember(@PathVariable Long memberId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, memberService.getMember(memberId));
    }

    @Override
    @PostMapping("/{memberId}/suspensions")
    public ApiResponse<MemberSuspensionRecord> suspendMember(
            @LoginMemberId Long adminId,
            @PathVariable Long memberId,
            @Valid @RequestBody AdminSuspensionRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, memberService.suspend(memberId, request.days(), request.note(), adminId));
    }

    @Override
    @PostMapping("/{memberId}/suspensions/lift")
    public ApiResponse<Void> liftSuspension(@LoginMemberId Long adminId, @PathVariable Long memberId) {
        memberService.lift(memberId, adminId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK);
    }
}
