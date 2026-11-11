package com.se201411.project.mooncake.common.format;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatTest {

    @Test
    void formatsVnd() {
        assertThat(MoneyFormat.vnd(new BigDecimal("125000"))).isEqualTo("125.000đ");
    }

    @Test
    void formatsSignedVnd() {
        assertThat(MoneyFormat.signedVnd(new BigDecimal("500"))).isEqualTo("+ 500đ");
        assertThat(MoneyFormat.signedVnd(new BigDecimal("-500"))).isEqualTo("- 500đ");
        assertThat(MoneyFormat.signedVnd(BigDecimal.ZERO)).isNull();
    }

    @Test
    void formatsDiscountLabel() {
        assertThat(MoneyFormat.discountLabel(new BigDecimal("5.00"))).isEqualTo("Giảm 5%");
        assertThat(MoneyFormat.discountLabel(BigDecimal.ZERO)).isNull();
        assertThat(MoneyFormat.discountLabel(null)).isNull();
    }
}
