package com.se201411.project.mooncake.product.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record CatalogVariantView(
    long id,
    String sku,
    String name,
    Integer weightGrams,
    String fillingLabel,
    BigDecimal unitPrice,
    String formattedPrice,
    String imageUrl,
    String imageAlt,
    PresetRecipeView presetRecipe
) {
    public CatalogVariantView {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(formattedPrice, "formattedPrice must not be null");
        Objects.requireNonNull(imageUrl, "imageUrl must not be null");
        Objects.requireNonNull(imageAlt, "imageAlt must not be null");
    }
}
