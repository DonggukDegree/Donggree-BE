package com.donggree.club.internal.infrastructure;

import com.donggree.club.internal.application.ClubImageStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 사진 파일을 디스크(운영은 Docker 볼륨)에 저장한다. 이름은 서버가 만든 UUID.확장자만 받아 경로 밖으로 나가지 않게 한다. */
@Component
public class DiskClubImageStorage implements ClubImageStorage {

    private static final Pattern NAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp)$");

    private final Path root;

    public DiskClubImageStorage(@Value("${club.image.dir:./data/club-images}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public void save(String storedName, byte[] bytes) {
        try {
            Files.createDirectories(root);
            Files.write(resolve(storedName), bytes);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public Optional<byte[]> load(String storedName) {
        if (!NAME.matcher(storedName).matches()) return Optional.empty();
        Path path = resolve(storedName);
        if (!Files.isRegularFile(path)) return Optional.empty();
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void delete(String storedName) {
        if (!NAME.matcher(storedName).matches()) return;
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Path resolve(String storedName) {
        if (!NAME.matcher(storedName).matches()) {
            throw new IllegalArgumentException("저장 이름 형식이 올바르지 않습니다");
        }
        return root.resolve(storedName);
    }
}
