package com.se201411.project.mooncake.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class UpdateCartItemForm {

    @NotNull(message = "Vui lòng nhập số lượng")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
