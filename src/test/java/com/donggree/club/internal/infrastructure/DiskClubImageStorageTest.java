package com.donggree.club.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiskClubImageStorageTest {

    private static final String NAME = "3f2b6c1e-5d4a-4b7e-9a1c-2e8f0d6b7c9a.webp";

    @TempDir
    Path dir;

    @Test
    void 저장_조회_삭제하고_서버가_만든_이름_형식만_받는다() {
        DiskClubImageStorage storage =
                new DiskClubImageStorage(dir.resolve("images").toString());

        storage.save(NAME, new byte[] {1, 2, 3});

        assertThat(storage.load(NAME)).hasValue(new byte[] {1, 2, 3});
        storage.delete(NAME);
        assertThat(storage.load(NAME)).isEmpty();
        assertThat(storage.load("../../etc/passwd")).isEmpty();
        assertThatThrownBy(() -> storage.save("../evil.webp", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
