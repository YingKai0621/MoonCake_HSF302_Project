package com.se201411.project.mooncake.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

public class CustomBoxForm {

    @NotNull(message = "Vui lòng chọn mẫu hộp")
    @Positive(message = "Mẫu hộp không hợp lệ")
    private Long boxId;

    @NotNull(message = "Số lượng hộp không được để trống")
    @Min(value = 1, message = "Số lượng hộp phải lớn hơn 0")
    private Integer boxQuantity = 1;

    private List<@Valid CustomCakeSelectionForm> cakes = new ArrayList<>();

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public Integer getBoxQuantity() {
        return boxQuantity;
    }

    public void setBoxQuantity(Integer boxQuantity) {
        this.boxQuantity = boxQuantity;
    }

    public List<CustomCakeSelectionForm> getCakes() {
        return cakes;
    }

    public void setCakes(List<CustomCakeSelectionForm> cakes) {
        this.cakes = cakes == null ? new ArrayList<>() : cakes;
    }
}
