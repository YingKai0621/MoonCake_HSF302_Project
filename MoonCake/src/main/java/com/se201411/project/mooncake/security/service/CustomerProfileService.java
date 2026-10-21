package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CustomerProfileService {

    CustomerProfile requireProfile(ShopperIdentity customerIdentity);
}
