package com.se201411.project.mooncake.security.repository;

import com.se201411.project.mooncake.entity.Account;

public interface AdminBootstrapRepository {

    boolean adminExistsUnderLock();

    boolean emailExists(String email);

    void save(Account account);
}
