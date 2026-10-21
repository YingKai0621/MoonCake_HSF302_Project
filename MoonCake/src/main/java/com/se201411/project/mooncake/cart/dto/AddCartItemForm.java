package com.se201411.project.mooncake.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AddCartItemForm {

    @NotNull(message = "Vui lòng chọn phiên bản sản phẩm")
    @Positive(message = "Phiên bản sản phẩm không hợp lệ")
    private Long variantId;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity = 1;

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
