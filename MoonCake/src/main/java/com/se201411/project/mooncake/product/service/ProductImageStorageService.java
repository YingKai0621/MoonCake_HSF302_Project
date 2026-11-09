package com.se201411.project.mooncake.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorageService {
    String store(MultipartFile file);
}
