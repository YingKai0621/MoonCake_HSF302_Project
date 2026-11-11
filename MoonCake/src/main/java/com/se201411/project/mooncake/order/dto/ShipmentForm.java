package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.enums.ShippingCarrier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ShipmentForm(
    @NotNull(message = "Vui lòng chọn đơn vị vận chuyển.")
    ShippingCarrier carrier,
    @Size(max = 50, message = "Mã vận đơn tối đa 50 ký tự.")
    String trackingCode,
    @NotNull(message = "Vui lòng chọn ngày giao dự kiến.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate estimatedDeliveryDate
) {
}
