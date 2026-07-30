package com.donggree.curriculum.internal.presentation.swagger;

import com.donggree.curriculum.internal.presentation.dto.CollegeResponse;
import com.donggree.curriculum.internal.presentation.dto.DepartmentResponse;
import com.donggree.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Admin-Department", description = "관리자의 단과대·학과 조회(필터 드롭다운용)")
public interface AdminDepartmentApi {

    @Operation(summary = "단과대 목록 조회", description = "단과대 목록을 id·이름과 함께 조회한다. 졸업 요건 세트 필터·등록 드롭다운의 선택지로 사용된다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<CollegeResponse>> getColleges();

    @Operation(
            summary = "학과 목록 조회",
            description = "학과 목록을 id·단과대(id·이름)·학과명과 함께 조회한다. collegeId를 주면 해당 단과대 소속만, 미지정이면 전체. "
                    + "사용자는 이름을 보고 고르고, 프론트는 id로 요청/응답한다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")})
    ApiResponse<List<DepartmentResponse>> getDepartments(@Parameter(description = "단과대 ID 필터(미지정=전체)") Long collegeId);
}
