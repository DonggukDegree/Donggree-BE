package com.donggree.user.internal.application;

/** SMTP 구현과 인증 유스케이스의 경계. 코드나 수신 주소를 로그에 남기지 않는다. */
public interface SchoolEmailSender {
    void send(String email, String code);
}
