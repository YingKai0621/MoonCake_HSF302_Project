package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.OwnershipMismatchException;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentCustomerServiceImpl implements CurrentCustomerService {

    @Override
    public ShopperIdentity requireCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw accessDenied();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof MoonCakeUserPrincipal userPrincipal)) {
            throw accessDenied();
        }

        if (userPrincipal.getRole() != AccountRole.CUSTOMER) {
            throw accessDenied();
        }

        Long customerId = userPrincipal.getCustomerId();
        if (customerId == null || customerId <= 0) {
            throw accessDenied();
        }

        return ShopperIdentity.forCustomer(customerId);
    }

    private OwnershipMismatchException accessDenied() {
        return new OwnershipMismatchException("Cần đăng nhập bằng tài khoản khách hàng");
    }
}
