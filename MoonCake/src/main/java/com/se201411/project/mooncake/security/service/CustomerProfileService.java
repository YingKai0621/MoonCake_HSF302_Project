package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CustomerProfileService {

    CustomerProfile requireProfile(ShopperIdentity customerIdentity);

    void updateFullName(ShopperIdentity customerIdentity, String fullName);

    void updateEmail(ShopperIdentity customerIdentity, String email);

    void updatePhone(ShopperIdentity customerIdentity, String phone);

    Customer customerReference(long customerId);

    long accountIdOf(long customerId);
}
