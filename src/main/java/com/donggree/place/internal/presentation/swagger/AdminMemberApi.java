package com.donggree.place.internal.presentation.swagger;

import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberDetail;
import com.donggree.place.internal.application.admin.AdminReportModels.MemberRow;
import com.donggree.place.internal.application.admin.AdminReportModels.Page;
import com.donggree.place.internal.presentation.dto.AdminSuspensionRequest;
import com.donggree.user.MemberSuspensionRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Member", description = "신고 대시보드 회원 관리·정지. ADMIN 이상")
public interface AdminMemberApi {

    @Operation(summary = "회원 검색", description = "학번·이름·닉네임 부분 일치, 정지 중 필터, 가입 최신순 20개씩. 받은 신고 수 포함.")
    ApiResponse<Page<MemberRow>> searchMembers(
            @Parameter(description = "검색어(생략 시 전체)") String keyword,
            @Parameter(description = "true면 지금 정지 중인 회원만") boolean suspended,
            @Parameter(description = "0부터 시작하는 페이지") int page);

    @Operation(summary = "회원 상세", description = "이름·학번·단과대·학과, 받은 신고(리뷰 신고 + 장소 신고 정지), 정지 이력.")
    ApiResponse<MemberDetail> getMember(@Parameter(description = "회원 ID") Long memberId);

    @Operation(summary = "회원 정지", description = "신고와 별개인 관리자 정지. 지금부터 days일, 정지 중이면 새 일수로 바꾼다. 기존 로그인도 끊긴다.")
    ApiResponse<MemberSuspensionRecord> suspendMember(
            Long adminId, @Parameter(description = "회원 ID") Long memberId, AdminSuspensionRequest request);

    @Operation(summary = "정지 해제", description = "정지 중인 회원을 바로 해제한다. 정지 중이 아니면 아무것도 하지 않는다.")
    ApiResponse<Void> liftSuspension(Long adminId, @Parameter(description = "회원 ID") Long memberId);
}
