package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.product.dto.AdminCatalogForms.CategoryForm;

import java.util.List;

public record ProductFormOptions(
    List<CategoryForm> categories,
    List<ProductTypeOption> productTypes,
    boolean giftBox
) {
    public ProductFormOptions {
        categories = List.copyOf(categories);
        productTypes = List.copyOf(productTypes);
    }
}
