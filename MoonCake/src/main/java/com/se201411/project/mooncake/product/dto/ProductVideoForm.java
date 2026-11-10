package com.se201411.project.mooncake.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductVideoForm(
    @NotNull(message = "Vui lòng chọn sản phẩm.")
    Long productId,
    @NotBlank(message = "Vui lòng nhập đường dẫn video.")
    String url,
    @NotBlank(message = "Vui lòng nhập mô tả.")
    String caption
) {
}
