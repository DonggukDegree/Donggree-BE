package com.donggree.club.internal.infrastructure;

import com.donggree.club.internal.application.ClubImageStorage;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 사진 파일을 Cloudflare R2(S3 호환 API)에 저장한다. 키는 서버가 만든 저장 이름 그대로 버킷 최상위에 둔다.
 *
 * <p>화면은 버킷에 연결한 공개 도메인(club.image.public-url)에서 사진을 직접 받는다. 저장 이름이 바뀌지 않으므로 1년 immutable 캐시를 객체에
 * 붙인다. R2는 AWS SDK 기본 체크섬 헤더 일부를 지원하지 않아 필요한 요청에만 계산한다.
 */
@Component
@ConditionalOnProperty(name = "club.image.storage", havingValue = "r2")
public class R2ClubImageStorage implements ClubImageStorage {

    static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client client;
    private final String bucket;

    @Autowired
    public R2ClubImageStorage(
            @Value("${club.image.r2.endpoint:}") String endpoint,
            @Value("${club.image.r2.bucket:}") String bucket,
            @Value("${club.image.r2.access-key-id:}") String accessKeyId,
            @Value("${club.image.r2.secret-access-key:}") String secretAccessKey) {
        this(
                createClient(
                        required(endpoint, "CLUB_R2_ENDPOINT"),
                        required(accessKeyId, "CLUB_R2_ACCESS_KEY_ID"),
                        required(secretAccessKey, "CLUB_R2_SECRET_ACCESS_KEY")),
                required(bucket, "CLUB_R2_BUCKET"));
    }

    R2ClubImageStorage(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void save(String storedName, String contentType, byte[] bytes) {
        client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(ClubImageStoredName.require(storedName))
                        .contentType(contentType)
                        .cacheControl(CACHE_CONTROL)
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    @Override
    public Optional<byte[]> load(String storedName) {
        if (!ClubImageStoredName.isValid(storedName)) return Optional.empty();
        try {
            return Optional.of(client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(storedName)
                            .build())
                    .asByteArray());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String storedName) {
        if (!ClubImageStoredName.isValid(storedName)) return;
        client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(storedName).build());
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private static S3Client createClient(String endpoint, String accessKeyId, String secretAccessKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .httpClient(UrlConnectionHttpClient.create())
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("R2 사진 저장소 설정이 없습니다: " + name);
        }
        return value.strip();
    }
}
