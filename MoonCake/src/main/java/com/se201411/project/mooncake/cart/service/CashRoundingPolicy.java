package com.se201411.project.mooncake.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CashRoundingPolicy {

    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1_000);

    private CashRoundingPolicy() {
    }

    public static BigDecimal roundToNearestThousand(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Số tiền cần làm tròn không được để trống");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Số tiền cần làm tròn không được âm");
        }

        return amount.divide(ONE_THOUSAND, 0, RoundingMode.HALF_UP)
            .multiply(ONE_THOUSAND);
    }
}
