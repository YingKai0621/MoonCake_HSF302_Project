package com.se201411.project.mooncake.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CustomerPhoneForm {

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
        regexp = "^0[0-9]{9}$",
        message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
    )
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
