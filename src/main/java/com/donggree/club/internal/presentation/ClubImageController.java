package com.donggree.club.internal.presentation;

import com.donggree.club.internal.application.ClubImageService;
import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubImageFile;
import com.donggree.club.internal.presentation.dto.ClubImageResponse;
import com.donggree.global.apiPayload.ApiResponse;
import com.donggree.global.apiPayload.code.GeneralSuccessCode;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.global.auth.LoginMemberId;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 동아리 사진 올리기(학생 인증 회원)와 내려주기(로그인 없이 공개, 이름이 바뀌지 않아 오래 캐시). */
@RestController
@RequestMapping("/api/clubs/images")
@RequiredArgsConstructor
public class ClubImageController {

    private final ClubImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ClubImageResponse> upload(
            @LoginMemberId Long memberId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK, ClubImageResponse.from(imageService.uploadByMember(memberId, bytes(file))));
    }

    @GetMapping("/{storedName}")
    public ResponseEntity<byte[]> download(@PathVariable String storedName) {
        ClubImageFile file = imageService.load(storedName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(
                        CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.bytes());
    }

    static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new GeneralException(ClubErrorCode.INVALID_IMAGE);
        }
    }
}
