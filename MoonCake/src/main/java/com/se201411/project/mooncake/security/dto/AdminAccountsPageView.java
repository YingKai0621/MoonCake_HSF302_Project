package com.se201411.project.mooncake.security.dto;

import java.util.List;

public record AdminAccountsPageView(List<AdminAccountRow> rows, boolean canManage) {

    public AdminAccountsPageView {
        rows = List.copyOf(rows);
    }
}
