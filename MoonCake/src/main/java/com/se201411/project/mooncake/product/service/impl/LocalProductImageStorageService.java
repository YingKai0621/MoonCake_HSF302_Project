package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.product.service.ProductImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalProductImageStorageService implements ProductImageStorageService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final String PUBLIC_PREFIX = "/uploads/products/";

    private final Path folder;

    public LocalProductImageStorageService(@Value("${mooncake.uploads.dir:uploads}") String uploadsDir) {
        this.folder = Path.of(uploadsDir, "products").toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("Chưa chọn ảnh để tải lên.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleViolationException("Ảnh quá lớn. Hãy chọn ảnh tối đa 5MB.");
        }

        String extension = detectExtension(file);
        if (extension == null) {
            throw new BusinessRuleViolationException("Chỉ nhận ảnh JPG, PNG hoặc WebP.");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path target = folder.resolve(fileName);
        try {
            Files.createDirectories(folder);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessRuleViolationException("Không lưu được ảnh. Hãy thử lại.");
        }
        return PUBLIC_PREFIX + fileName;
    }

    private String detectExtension(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.readNBytes(head, 0, head.length);
        } catch (IOException ex) {
            return null;
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return "png";
        }
        boolean riff = read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F';
        if (riff && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
