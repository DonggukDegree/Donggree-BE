package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class R2ClubImageStorageTest {

    private static final String NAME = "3f2b6c1e-5d4a-4b7e-9a1c-2e8f0d6b7c9a.webp";

    private final S3Client client = mock(S3Client.class);
    private final R2ClubImageStorage storage = new R2ClubImageStorage(client, "donggree-club-images");

    @Test
    void 저장_이름을_키로_형식과_오래_캐시를_붙여_올린다() {
        storage.save(NAME, "image/webp", new byte[] {1, 2, 3});

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("donggree-club-images");
        assertThat(request.getValue().key()).isEqualTo(NAME);
        assertThat(request.getValue().contentType()).isEqualTo("image/webp");
        assertThat(request.getValue().cacheControl()).isEqualTo("public, max-age=31536000, immutable");
    }

    @Test
    void 없는_사진은_빈_값이고_삭제는_키로_요청한다() {
        given(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] {7}))
                .willThrow(NoSuchKeyException.builder().build());

        assertThat(storage.load(NAME)).hasValue(new byte[] {7});
        assertThat(storage.load(NAME)).isEmpty();
        storage.delete(NAME);

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo(NAME);
    }

    @Test
    void 서버가_만든_이름_형식만_받는다() {
        S3Client untouched = mock(S3Client.class);
        R2ClubImageStorage guarded = new R2ClubImageStorage(untouched, "donggree-club-images");

        assertThat(guarded.load("../secret")).isEmpty();
        guarded.delete("other/key.webp");
        assertThatThrownBy(() -> guarded.save("evil.webp", "image/webp", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(untouched);
    }

    @Test
    void 설정이_비면_시작하지_않는다() {
        assertThatThrownBy(() -> new R2ClubImageStorage("https://account.r2.cloudflarestorage.com", "", "id", "secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLUB_R2_BUCKET");
        verify(client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
