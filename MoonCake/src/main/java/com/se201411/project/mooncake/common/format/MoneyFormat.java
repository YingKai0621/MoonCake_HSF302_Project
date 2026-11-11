package com.se201411.project.mooncake.common.format;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormat {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private MoneyFormat() {
    }

    public static String vnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }

    public static String signedVnd(BigDecimal amount) {
        if (amount.signum() == 0) {
            return null;
        }
        if (amount.signum() > 0) {
            return "+ " + vnd(amount);
        }
        return "- " + vnd(amount.abs());
    }

    public static String discountLabel(BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.signum() == 0) {
            return null;
        }
        return "Giảm " + discountPercent.stripTrailingZeros().toPlainString() + "%";
    }
}
