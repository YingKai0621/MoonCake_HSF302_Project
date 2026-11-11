package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.dto.AccountLockResult;
import com.se201411.project.mooncake.security.dto.AdminCustomerDetailPageView;
import com.se201411.project.mooncake.security.dto.AdminCustomerListPageView;

public interface CustomerAdminService {

    AdminCustomerListPageView customersPage(String keyword, int page);

    AdminCustomerDetailPageView customerPage(long customerId);

    AccountLockResult lock(long customerId, boolean cancelUnpaidOrders);

    void unlock(long customerId);
}
