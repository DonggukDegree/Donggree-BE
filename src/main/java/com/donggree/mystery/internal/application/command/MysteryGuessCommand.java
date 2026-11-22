package com.donggree.mystery.internal.application.command;

import com.donggree.mystery.internal.domain.MysteryData.Guess;
import java.time.LocalDate;
import java.util.UUID;

public record MysteryGuessCommand(LocalDate gameDate, UUID requestId, Guess guess) {}
