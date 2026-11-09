package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.product.dto.AdminCatalogForms.BoxForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ImageForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;

import java.util.List;

public record AdminProductDetail(
    ProductForm product,
    String typeLabel,
    boolean typeLocked,
    boolean emptyBox,
    String webLabel,
    String webTone,
    List<Sku> skus,
    List<BoxForm> boxes
) {
    public record Sku(
        VariantForm form,
        String imageUrl,
        List<ImageForm> images,
        int stock,
        String webLabel,
        String webTone
    ) {
    }
}
