package com.donggree.club.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubImageReadModel;
import com.donggree.club.internal.domain.ClubImage;
import com.donggree.club.internal.domain.ClubImageRepository;
import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.UserLookupService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ClubImageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1};

    private final ClubImageRepository images = mock(ClubImageRepository.class);
    private final ClubImageStorage storage = mock(ClubImageStorage.class);
    private final UserLookupService users = mock(UserLookupService.class);
    private final ClubImageService service = new ClubImageService(
            images,
            storage,
            users,
            () -> LocalDateTime.of(2026, 11, 27, 12, 0),
            new ClubImageUrls("https://img.example.com"));

    @Test
    void 학생_인증_회원의_사진을_형식_확인_후_저장한다() {
        given(users.isStudentVerified(7L)).willReturn(true);
        given(images.save(any(ClubImage.class))).willAnswer(invocation -> {
            ClubImage image = invocation.getArgument(0);
            ReflectionTestUtils.setField(image, "id", 21L);
            return image;
        });

        ClubImageReadModel uploaded = service.uploadByMember(7L, PNG);

        assertThat(uploaded.id()).isEqualTo(21L);
        assertThat(uploaded.url()).startsWith("https://img.example.com/").endsWith(".png");
        verify(storage).save(anyString(), eq("image/png"), any());
    }

    @Test
    void 미인증_회원과_허용하지_않는_형식은_거절한다() {
        given(users.isStudentVerified(7L)).willReturn(false);
        given(users.isStudentVerified(8L)).willReturn(true);

        assertThatThrownBy(() -> service.uploadByMember(7L, PNG))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.VERIFICATION_REQUIRED));
        assertThatThrownBy(() -> service.uploadByMember(8L, "GIF89a-not-allowed".getBytes()))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ClubErrorCode.INVALID_IMAGE));
        verify(storage, never()).save(anyString(), anyString(), any());
    }

    @Test
    void 관리자는_배너_사진을_올린다() {
        given(images.save(any(ClubImage.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.uploadByAdmin(1L, PNG, ClubImagePurpose.BANNER);

        verify(images)
                .save(org.mockito.ArgumentMatchers.argThat(image -> image.getPurpose() == ClubImagePurpose.BANNER));
    }
}
