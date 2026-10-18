package com.donggree.user.internal.application.command;

public record VerifySchoolEmailCodeCommand(String localPart, String code) {}
