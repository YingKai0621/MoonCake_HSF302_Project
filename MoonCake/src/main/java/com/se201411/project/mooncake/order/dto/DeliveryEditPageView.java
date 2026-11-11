package com.se201411.project.mooncake.order.dto;

import java.util.List;

public record DeliveryEditPageView(
    String orderCode,
    List<AddressOption> provinces,
    List<AddressOption> wards
) {
    public DeliveryEditPageView {
        provinces = List.copyOf(provinces);
        wards = List.copyOf(wards);
    }
}
