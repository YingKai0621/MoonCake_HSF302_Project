package com.se201411.project.mooncake.product.dto;

import jakarta.validation.constraints.*;
import com.se201411.project.mooncake.enums.ProductType;

import java.math.BigDecimal;

public final class AdminCatalogForms {
    private AdminCatalogForms() {
    }

    public record CategoryForm(Long id, @NotBlank @Size(max = 160) String name,
                               @NotBlank @Size(max = 160) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
                               boolean active) {
    }

    public record ProductForm(Long id, @Positive long categoryId, @NotNull ProductType productType,
                              @NotBlank @Size(max = 220) String name,
                              @NotBlank @Size(max = 180) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
                              @NotBlank @Size(max = 600) String shortDescription, String detailedDescription,
                              @Size(max = 700) String mainIngredients, @Size(max = 350) String shelfLifeInfo,
                              @Size(max = 600) String storageGuidance, boolean active) {
    }

    public record VariantForm(Long id, @Positive long productId, @NotBlank @Size(max = 80) String sku,
                              @NotBlank @Size(max = 200) String name, @Positive Integer weightGrams,
                              @Size(max = 180) String fillingLabel,
                              @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 0) BigDecimal unitPrice,
                              boolean active) {
    }

    public record BoxForm(Long id, @Positive long variantId, @Positive int capacityCakes,
                          @Size(max = 180) String material, @Size(max = 500) String notes, boolean active) {
    }

    public record ImageForm(Long id, @Positive long variantId,
                            @NotBlank @Size(max = 1000) @Pattern(regexp = "(?:https?://[^\\s]+|/images/[^\\s]+)") String imageUrl,
                            @Size(max = 250) String altText, @Min(0) int sortOrder, boolean primary) {
    }
}
