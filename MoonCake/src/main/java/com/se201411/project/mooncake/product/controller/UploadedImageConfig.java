package com.se201411.project.mooncake.product.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadedImageConfig implements WebMvcConfigurer {
    private final String productsFolder;

    public UploadedImageConfig(@Value("${mooncake.uploads.dir:uploads}") String uploadsDir) {
        String folder = Path.of(uploadsDir, "products").toAbsolutePath().normalize().toUri().toString();
        if (!folder.endsWith("/")) {
            folder = folder + "/";
        }
        this.productsFolder = folder;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/products/**").addResourceLocations(productsFolder);
    }
}
