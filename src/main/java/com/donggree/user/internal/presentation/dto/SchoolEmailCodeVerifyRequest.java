package com.donggree.user.internal.presentation.dto;

import com.donggree.user.internal.application.command.VerifySchoolEmailCodeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SchoolEmailCodeVerifyRequest(
        @NotBlank @Size(min = 6, max = 30) String localPart, @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {
    public VerifySchoolEmailCodeCommand toCommand() {
        return new VerifySchoolEmailCodeCommand(localPart, code);
    }
}
