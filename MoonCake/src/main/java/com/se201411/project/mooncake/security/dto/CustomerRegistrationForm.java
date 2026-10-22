package com.se201411.project.mooncake.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerRegistrationForm {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 8, max = 64, message = "Mật khẩu phải có từ 8 đến 64 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @Size(min = 8, max = 64, message = "Mật khẩu xác nhận phải có từ 8 đến 64 ký tự")
    private String confirmPassword;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 40, message = "Họ tên không được vượt quá 40 ký tự")
    @Pattern(
        regexp = "^[\\p{L}\\s]+$",
        message = "Họ tên chỉ được chứa chữ cái và khoảng trắng"
    )
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
        regexp = "^0[0-9]{9}$",
        message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0"
    )
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null) {
            this.email = null;
            return;
        }
        this.email = email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
