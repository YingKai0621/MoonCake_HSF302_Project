package com.se201411.project.mooncake.product;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.product.service.impl.LocalProductImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalProductImageStorageServiceTest {
    private static final byte[] PNG_BYTES = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };

    @TempDir
    Path uploads;

    @Test
    void storesRealImageUnderRandomName() throws Exception {
        LocalProductImageStorageService storage = new LocalProductImageStorageService(uploads.toString());
        MockMultipartFile file = new MockMultipartFile("imageFile", "../../evil.html", "image/png", PNG_BYTES);

        String url = storage.store(file);

        assertThat(url).startsWith("/uploads/products/").endsWith(".png");
        assertThat(url).doesNotContain("evil");
        String fileName = url.substring("/uploads/products/".length());
        assertThat(Files.readAllBytes(uploads.resolve("products").resolve(fileName))).isEqualTo(PNG_BYTES);
    }

    @Test
    void rejectsFileThatIsNotAnImage() {
        LocalProductImageStorageService storage = new LocalProductImageStorageService(uploads.toString());
        MockMultipartFile file = new MockMultipartFile("imageFile", "photo.png", "image/png", "<script>".getBytes());

        assertThatThrownBy(() -> storage.store(file))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("JPG, PNG hoặc WebP");
    }

    @Test
    void rejectsEmptyUpload() {
        LocalProductImageStorageService storage = new LocalProductImageStorageService(uploads.toString());
        MockMultipartFile file = new MockMultipartFile("imageFile", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storage.store(file))
            .isInstanceOf(BusinessRuleViolationException.class);
    }
}
