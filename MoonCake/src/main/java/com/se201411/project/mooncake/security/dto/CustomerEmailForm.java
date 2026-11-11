package com.se201411.project.mooncake.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerEmailForm {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email tối đa 254 ký tự")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
