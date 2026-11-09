package com.se201411.project.mooncake.product.dto;

import jakarta.validation.constraints.*;

public record AdminPresetForm(
    Long id,
    @Positive(message = "Vui lòng chọn hộp quà.")
    long giftSetVariantId,
    @Positive(message = "Vui lòng chọn vỏ hộp.")
    long boxId
) {
}
