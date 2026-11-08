package com.se201411.project.mooncake.order.dto;

public record PaymentReturnResult(
    boolean verified,
    String orderCode,
    boolean completedOnGateway,
    boolean paid
) {
    public static PaymentReturnResult unverified() {
        return new PaymentReturnResult(false, null, false, false);
    }
}
