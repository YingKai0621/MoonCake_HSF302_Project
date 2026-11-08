package com.se201411.project.mooncake.order.dto;

public record DeliveryAddressParts(
    String streetAddress,
    String wardCode,
    String provinceCode
) {
}
