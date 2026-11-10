package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.ProductVariant;
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
    PresetRecipeView presetRecipe,
    int stock
) {

    /** Entity -> DTO. Picture, PRESET recipe and stock are worked out by the service. */
    public static CatalogVariantView entityToDto(
        ProductVariant variant,
        String imageUrl,
        String imageAlt,
        PresetRecipeView presetRecipe,
        int stock
    ) {
        return new CatalogVariantView(
            variant.getId(),
            variant.getSku(),
            variant.getName(),
            variant.getWeightGrams(),
            variant.getFillingLabel(),
            variant.getUnitPrice(),
            MoneyFormat.vnd(variant.getUnitPrice()),
            imageUrl,
            imageAlt,
            presetRecipe,
            stock
        );
    }

    public static final int UNKNOWN_STOCK = -1;
    public static final int LOW_STOCK = 5;

    public CatalogVariantView(long id, String sku, String name, Integer weightGrams, String fillingLabel,
                              BigDecimal unitPrice, String formattedPrice, String imageUrl, String imageAlt,
                              PresetRecipeView presetRecipe) {
        this(id, sku, name, weightGrams, fillingLabel, unitPrice, formattedPrice, imageUrl, imageAlt,
            presetRecipe, UNKNOWN_STOCK);
    }

    public boolean soldOut() {
        return stock == 0;
    }

    public boolean lowStock() {
        return stock > 0 && stock <= LOW_STOCK;
    }

    public CatalogVariantView {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(formattedPrice, "formattedPrice must not be null");
        Objects.requireNonNull(imageUrl, "imageUrl must not be null");
        Objects.requireNonNull(imageAlt, "imageAlt must not be null");
    }
}
