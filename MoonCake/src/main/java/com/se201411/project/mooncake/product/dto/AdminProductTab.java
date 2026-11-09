package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.enums.ProductType;

public record AdminProductTab(ProductType type, String label, long count, boolean selected) {
}
