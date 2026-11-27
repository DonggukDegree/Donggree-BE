package com.donggree.club.internal.application;

import java.util.Optional;

/** 사진 파일 저장소. 운영은 홈서버 디스크(볼륨)를 쓴다. */
public interface ClubImageStorage {

    void save(String storedName, byte[] bytes);

    Optional<byte[]> load(String storedName);

    /** 없으면 아무것도 하지 않는다. */
    void delete(String storedName);
}
