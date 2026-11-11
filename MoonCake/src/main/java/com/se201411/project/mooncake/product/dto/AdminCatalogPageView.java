package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.product.dto.AdminCatalogForms.CategoryForm;

import java.util.List;

public record AdminCatalogPageView(
    List<AdminProductRow> products,
    List<AdminProductTab> tabs,
    List<CategoryForm> categories
) {
    public AdminCatalogPageView {
        products = List.copyOf(products);
        tabs = List.copyOf(tabs);
        categories = List.copyOf(categories);
    }
}
