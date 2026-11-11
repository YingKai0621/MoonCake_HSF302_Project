package com.se201411.project.mooncake.security.dto;

import java.util.List;

public record AdminCustomerListPageView(
    List<AdminCustomerRow> rows,
    String keyword,
    int pageNumber,
    boolean hasNext
) {

    public AdminCustomerListPageView {
        rows = List.copyOf(rows);
    }
}
