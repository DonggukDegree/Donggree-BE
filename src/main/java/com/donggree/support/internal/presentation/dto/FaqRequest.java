package com.donggree.support.internal.presentation.dto;

import com.donggree.support.internal.application.command.FaqCommand;
import com.donggree.support.internal.domain.enums.FaqTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * FAQ 등록/수정 요청. POST(생성)와 PUT(전체 수정)에 공통으로 사용한다.
 * tag는 SERVICE(서비스)·COMMON(공통)·MAJOR(전공) 중 하나를 반드시 지정해야 한다.
 * 기본값으로 대신 채우지 않는다 — 분류를 빠뜨린 글이 조용히 한 태그에 쌓이는 것을 막기 위함이다.
 */
public record FaqRequest(
        @NotNull(message = "태그는 필수입니다.")
                @Schema(
                        example = "SERVICE",
                        description = "태그(SERVICE=서비스, COMMON=공통, MAJOR=전공). 필수",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                FaqTag tag,
        @NotBlank(message = "제목은 필수입니다.")
                @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
                @Schema(example = "성적표는 어디서 받나요?")
                String title,
        @NotBlank(message = "본문은 필수입니다.") @Schema(example = "nDRIMS > 성적 > 성적표출력에서 받을 수 있어요.") String content) {

    public FaqCommand toCommand() {
        return new FaqCommand(tag, title, content);
    }
}
