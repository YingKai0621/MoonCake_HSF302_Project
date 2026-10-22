package com.se201411.project.mooncake.product.dto;

import jakarta.validation.constraints.*;

public record AdminPresetForm(Long id, @Positive long giftSetVariantId, @Positive long boxId) {
}
