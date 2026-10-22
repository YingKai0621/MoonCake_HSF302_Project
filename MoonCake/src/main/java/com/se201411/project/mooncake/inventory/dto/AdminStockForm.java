package com.se201411.project.mooncake.inventory.dto;

import jakarta.validation.constraints.*;

public record AdminStockForm(@Positive long variantId, int quantity,
                             @NotBlank @Size(max = 500) String reason,
                             @NotBlank @Pattern(regexp = "[a-f0-9-]{36}") String operationKey) {
}
