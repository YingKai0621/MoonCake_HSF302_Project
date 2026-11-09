package com.se201411.project.mooncake.product.dto;

import java.math.BigDecimal;

public record GiftBoxRow(
    long id,
    String name,
    String imageUrl,
    BigDecimal price,
    String webLabel,
    String webTone,
    int doneSteps
) {
}
