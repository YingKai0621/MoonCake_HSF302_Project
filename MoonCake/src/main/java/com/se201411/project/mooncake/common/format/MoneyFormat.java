package com.se201411.project.mooncake.common.format;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats VND amounts the same way everywhere, so DTO mapping methods can use it.
 */
public final class MoneyFormat {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private MoneyFormat() {
    }

    /** 125000 -> "125.000đ" */
    public static String vnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }

    /** 500 -> "+ 500đ", -500 -> "- 500đ", 0 -> null (nothing to show). */
    public static String signedVnd(BigDecimal amount) {
        if (amount.signum() == 0) {
            return null;
        }
        if (amount.signum() > 0) {
            return "+ " + vnd(amount);
        }
        return "- " + vnd(amount.abs());
    }

    /** 5 -> "Giảm 5%", null or 0 -> null. */
    public static String discountLabel(BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.signum() == 0) {
            return null;
        }
        return "Giảm " + discountPercent.stripTrailingZeros().toPlainString() + "%";
    }
}
