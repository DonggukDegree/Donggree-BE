package com.donggree.user.internal.application;

public interface SchoolEmailCodes {
    String generate();

    String hash(Long memberId, String email, String code);

    boolean matches(Long memberId, String email, String code, String hash);
}
