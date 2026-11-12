package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.dto.AdminAccountForm;
import com.se201411.project.mooncake.security.dto.AdminAccountsPageView;

public interface AdminAccountService {

    AdminAccountsPageView page();

    void create(AdminAccountForm form);

    void lock(long accountId);

    void unlock(long accountId);
}
