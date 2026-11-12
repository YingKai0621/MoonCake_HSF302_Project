package com.se201411.project.mooncake.order.dto;

public record CustomerOrderCount(
    long customerId,
    long total,
    long cancelled
) {

    public static CustomerOrderCount none(long customerId) {
        return new CustomerOrderCount(customerId, 0, 0);
    }
}
