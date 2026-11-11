package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CustomerProfileService {

    CustomerProfile requireProfile(ShopperIdentity customerIdentity);

    AccountPageView accountPage(ShopperIdentity customerIdentity);

    ProfileUpdateForm profileForm(ShopperIdentity customerIdentity);

    void updateProfile(ShopperIdentity customerIdentity, ProfileUpdateForm form);

    Customer customerReference(long customerId);

    long accountIdOf(long customerId);
}
