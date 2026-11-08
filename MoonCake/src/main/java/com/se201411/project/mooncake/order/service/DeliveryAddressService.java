package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.AddressOption;
import com.se201411.project.mooncake.order.dto.DeliveryAddressParts;

import java.util.List;
import java.util.Optional;

public interface DeliveryAddressService {

    List<AddressOption> getProvinces();

    List<AddressOption> getWards();

    Optional<DeliveryAddressParts> parseShippingAddress(String shippingAddress);

    String buildShippingAddress(
        String streetAddress,
        String wardCode,
        String provinceCode
    );
}
