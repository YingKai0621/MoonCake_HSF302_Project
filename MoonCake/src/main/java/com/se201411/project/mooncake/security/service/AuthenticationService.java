package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;

public interface AuthenticationService {

    void registerCustomer(CustomerRegistrationForm form);
}
