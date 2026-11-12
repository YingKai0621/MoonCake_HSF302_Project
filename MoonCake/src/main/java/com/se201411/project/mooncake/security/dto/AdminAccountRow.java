package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.common.format.VietnamTime;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.AccountStatus;

public record AdminAccountRow(
    long accountId,
    String email,
    String createdOn,
    boolean active,
    boolean superAdmin,
    boolean currentUser
) {

    public static AdminAccountRow entityToDto(Account account, long superAdminId, long currentAdminId) {
        return new AdminAccountRow(
            account.getId(),
            account.getEmail(),
            VietnamTime.date(account.getCreatedAt()),
            account.getStatus() == AccountStatus.ACTIVE,
            account.getId() == superAdminId,
            account.getId() == currentAdminId
        );
    }

    public boolean lockable() {
        return !superAdmin && !currentUser;
    }
}
