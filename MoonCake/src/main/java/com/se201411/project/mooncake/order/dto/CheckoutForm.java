package com.se201411.project.mooncake.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CheckoutForm {

    @NotNull(message = "Không tìm thấy giỏ hàng")
    @Positive(message = "Giỏ hàng không hợp lệ")
    private Long cartId;

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 40, message = "Tên người nhận không được vượt quá 40 ký tự")
    @Pattern(
        regexp = "^[\\p{L}\\s]+$",
        message = "Tên người nhận chỉ được chứa chữ cái và khoảng trắng"
    )
    private String recipientName;

    @NotBlank(message = "Vui lòng nhập số điện thoại người nhận")
    @Pattern(
        regexp = "^0[0-9]{9}$",
        message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng 0"
    )
    private String recipientPhone;

    @NotBlank(message = "Vui lòng chọn tỉnh hoặc thành phố")
    @Size(max = 30, message = "Tỉnh hoặc thành phố không hợp lệ")
    private String provinceCode;

    @NotBlank(message = "Vui lòng chọn phường, xã hoặc đặc khu")
    @Size(max = 10, message = "Phường, xã hoặc đặc khu không hợp lệ")
    private String wardCode;

    @NotBlank(message = "Vui lòng nhập số nhà và tên đường")
    @Size(max = 120, message = "Số nhà và tên đường không được vượt quá 120 ký tự")
    private String streetAddress;

    @Size(max = 200, message = "Ghi chú không được vượt quá 200 ký tự")
    private String orderNote;

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getOrderNote() {
        return orderNote;
    }

    public void setOrderNote(String orderNote) {
        this.orderNote = orderNote;
    }
}
