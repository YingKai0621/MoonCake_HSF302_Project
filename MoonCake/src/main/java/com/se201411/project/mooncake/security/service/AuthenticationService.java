package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface AuthenticationService {

    void registerCustomer(CustomerRegistrationForm form);

    void changePassword(
        ShopperIdentity customerIdentity,
        String currentPassword,
        String newPassword
    );
}
