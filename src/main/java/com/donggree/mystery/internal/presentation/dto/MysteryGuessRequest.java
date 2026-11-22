package com.donggree.mystery.internal.presentation.dto;

import com.donggree.mystery.internal.application.command.MysteryGuessCommand;
import com.donggree.mystery.internal.domain.MysteryData.Guess;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record MysteryGuessRequest(
        @NotNull LocalDate gameDate,
        @NotNull UUID requestId,
        @NotBlank @Size(max = 40) String departmentId,
        @NotBlank @Size(max = 40) String buildingId,
        @NotBlank @Size(max = 40) String methodId) {
    public MysteryGuessCommand toCommand() {
        return new MysteryGuessCommand(gameDate, requestId, new Guess(departmentId, buildingId, methodId));
    }
}
