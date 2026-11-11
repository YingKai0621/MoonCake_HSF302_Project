package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

import java.util.Collection;
import java.util.Set;

public interface CustomerProfileService {

    CustomerProfile requireProfile(ShopperIdentity customerIdentity);

    Customer customerReference(long customerId);

    long accountIdOf(long customerId);

    AccountPageView accountPage(ShopperIdentity customerIdentity);

    ProfileUpdateForm profileForm(ShopperIdentity customerIdentity);

    void updateProfile(ShopperIdentity customerIdentity, ProfileUpdateForm form);

    Set<Long> disabledCustomerIds(Collection<Long> customerIds);

    void requireActiveAccount(ShopperIdentity customerIdentity);

    boolean disableAccount(long customerId);

    boolean enableAccount(long customerId);

    long accountIdOf(long customerId);
}
