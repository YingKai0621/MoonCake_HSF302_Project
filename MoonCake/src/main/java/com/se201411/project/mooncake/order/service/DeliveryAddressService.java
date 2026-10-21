package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.AddressOption;

import java.util.List;

public interface DeliveryAddressService {

    List<AddressOption> getProvinces();

    List<AddressOption> getWards();

    String buildShippingAddress(
        String streetAddress,
        String wardCode,
        String provinceCode
    );
}
