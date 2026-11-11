package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.entity.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateForm {

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

    public static ProfileUpdateForm entityToDto(Customer customer) {
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName(customer.getFullName());
        form.setPhone(customer.getPhone());
        return form;
    }

    public void dtoToEntity(Customer customer) {
        customer.setFullName(fullName.trim());
        customer.setPhone(phone);
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
