package com.donggree.club.internal.application;

import java.util.Optional;

/** 사진 파일 저장소. club.image.storage로 디스크(기본)와 Cloudflare R2 중 하나를 쓴다. */
public interface ClubImageStorage {

    void save(String storedName, String contentType, byte[] bytes);

    Optional<byte[]> load(String storedName);

    /** 없으면 아무것도 하지 않는다. */
    void delete(String storedName);
}
