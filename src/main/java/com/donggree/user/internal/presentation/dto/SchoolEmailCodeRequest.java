package com.donggree.user.internal.presentation.dto;

import com.donggree.user.internal.application.command.RequestSchoolEmailCodeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolEmailCodeRequest(@NotBlank @Size(min = 6, max = 30) String localPart) {
    public RequestSchoolEmailCodeCommand toCommand() {
        return new RequestSchoolEmailCodeCommand(localPart);
    }
}
