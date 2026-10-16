package com.donggree.global.apiPayload.code;

import org.springframework.http.HttpStatus;

@org.springframework.modulith.NamedInterface("api")
public interface BaseErrorCode {
    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
