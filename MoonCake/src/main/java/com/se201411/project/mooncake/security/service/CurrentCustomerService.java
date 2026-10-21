package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CurrentCustomerService {

    ShopperIdentity requireCurrentCustomer();
}
