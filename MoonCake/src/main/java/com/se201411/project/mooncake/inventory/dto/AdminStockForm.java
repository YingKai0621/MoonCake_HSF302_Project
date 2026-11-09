package com.se201411.project.mooncake.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminStockForm(
    @Positive(message = "Vui lòng chọn sản phẩm.")
    long variantId,
    @Min(value = -100000, message = "Số lượng quá lớn.")
    @Max(value = 100000, message = "Số lượng tối đa 100.000.")
    int quantity,
    @NotBlank(message = "Vui lòng nhập ghi chú.")
    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
    String reason,
    @NotBlank(message = "Phiên làm việc đã hết hạn. Hãy tải lại trang.")
    @Pattern(regexp = "[a-f0-9-]{36}", message = "Phiên làm việc đã hết hạn. Hãy tải lại trang.")
    String operationKey
) {
}
