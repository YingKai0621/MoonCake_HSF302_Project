package com.se201411.project.mooncake.dashboard.dto;

import com.se201411.project.mooncake.order.dto.ProductSalesSummary;
import java.math.BigDecimal;

public record TopProductView(
    String name,
    Long quantity,
    BigDecimal revenue
) {
    public static TopProductView fromSummary(ProductSalesSummary summary) {
        return new TopProductView(summary.name(), summary.quantity(), summary.revenue());
    }
}
