package com.se201411.project.mooncake.dashboard.dto;

import java.math.BigDecimal;

public record TopProductView(
    String name,
    Long quantity,
    BigDecimal revenue
) {
}
