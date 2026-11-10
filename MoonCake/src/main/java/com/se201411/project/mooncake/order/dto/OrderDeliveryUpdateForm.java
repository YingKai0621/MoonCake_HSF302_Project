package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.entity.SalesOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OrderDeliveryUpdateForm {

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

    private String currentShippingAddress;

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

    public String getCurrentShippingAddress() {
        return currentShippingAddress;
    }

    public void setCurrentShippingAddress(String currentShippingAddress) {
        this.currentShippingAddress = currentShippingAddress;
    }

    /**
     * Entity -> DTO: fills the edit form from the order. The service splits the saved
     * address into street/ward/province afterwards.
     */
    public static OrderDeliveryUpdateForm entityToDto(SalesOrder order) {
        OrderDeliveryUpdateForm form = new OrderDeliveryUpdateForm();
        form.setRecipientName(order.getRecipientName());
        form.setRecipientPhone(order.getRecipientPhone());
        form.setOrderNote(order.getOrderNote());
        form.setCurrentShippingAddress(order.getShippingAddressSnapshot());
        return form;
    }
}
