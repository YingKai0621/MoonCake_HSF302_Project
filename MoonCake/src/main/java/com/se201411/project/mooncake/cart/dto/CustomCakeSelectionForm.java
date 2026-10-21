package com.se201411.project.mooncake.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CustomCakeSelectionForm {

    @NotNull(message = "Thiếu mã bánh")
    @Positive(message = "Mã bánh không hợp lệ")
    private Long variantId;

    @NotNull(message = "Số lượng bánh không được để trống")
    @Min(value = 0, message = "Số lượng bánh không được âm")
    private Integer quantity = 0;

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
