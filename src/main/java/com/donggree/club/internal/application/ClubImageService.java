package com.donggree.club.internal.application;

import com.donggree.club.internal.application.exception.ClubErrorCode;
import com.donggree.club.internal.application.readmodel.ClubImageFile;
import com.donggree.club.internal.application.readmodel.ClubImageReadModel;
import com.donggree.club.internal.domain.ClubImage;
import com.donggree.club.internal.domain.ClubImageRepository;
import com.donggree.club.internal.domain.ImageFormat;
import com.donggree.club.internal.domain.enums.ClubImagePurpose;
import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사진 올리기·내려주기. 형식은 파일 앞부분으로 판단하고(JPG·PNG·WEBP, 5MB 이하), 파일은 추측할 수 없는 이름으로 저장한다.
 * 회원은 학생 인증 후 동아리 사진만 올리고, 관리자는 동아리·배너 사진을 올린다. 사진 조회는 로그인 없이 공개한다.
 */
@Service
@RequiredArgsConstructor
public class ClubImageService {

    private final ClubImageRepository imageRepository;
    private final ClubImageStorage storage;
    private final UserLookupService userLookupService;
    private final ClubClock clock;
    private final ClubImageUrls imageUrls;

    @Transactional
    public ClubImageReadModel uploadByMember(Long memberId, byte[] bytes) {
        if (!userLookupService.isStudentVerified(memberId)) {
            throw new GeneralException(ClubErrorCode.VERIFICATION_REQUIRED);
        }
        return upload(bytes, ClubImagePurpose.CLUB, memberId);
    }

    @Transactional
    public ClubImageReadModel uploadByAdmin(Long adminId, byte[] bytes, ClubImagePurpose purpose) {
        return upload(bytes, purpose == null ? ClubImagePurpose.CLUB : purpose, adminId);
    }

    @Transactional(readOnly = true)
    public ClubImageFile load(String storedName) {
        ClubImage image = imageRepository
                .findByStoredName(storedName)
                .orElseThrow(() -> new GeneralException(ClubErrorCode.IMAGE_NOT_FOUND));
        byte[] bytes = storage.load(storedName).orElseThrow(() -> new GeneralException(ClubErrorCode.IMAGE_NOT_FOUND));
        return new ClubImageFile(image.getContentType(), bytes);
    }

    // 행을 먼저 저장해 이름을 정하고 파일을 쓴다. 파일 쓰기에 실패하면 트랜잭션이 롤백돼 행도 남지 않는다.
    private ClubImageReadModel upload(byte[] bytes, ClubImagePurpose purpose, Long uploaderId) {
        if (bytes == null || bytes.length == 0 || bytes.length > ImageFormat.MAX_BYTES) {
            throw new GeneralException(ClubErrorCode.INVALID_IMAGE);
        }
        ImageFormat format =
                ImageFormat.detect(bytes).orElseThrow(() -> new GeneralException(ClubErrorCode.INVALID_IMAGE));
        ClubImage image =
                imageRepository.save(ClubImage.upload(format, bytes.length, purpose, uploaderId, clock.now()));
        storage.save(image.getStoredName(), image.getContentType(), bytes);
        return ClubImageReadModel.from(image, imageUrls.of(image));
    }
}
