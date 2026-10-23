package com.se201411.project.mooncake.cart.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CashRoundingPolicyTest {

    @Test
    void roundsDownWhenRemainderIsBelowFiveHundred() {
        assertThat(CashRoundingPolicy.roundToNearestThousand(BigDecimal.valueOf(120_499)))
            .isEqualByComparingTo("120000");
    }

    @Test
    void roundsUpWhenRemainderIsFiveHundredOrMore() {
        assertThat(CashRoundingPolicy.roundToNearestThousand(BigDecimal.valueOf(120_500)))
            .isEqualByComparingTo("121000");
        assertThat(CashRoundingPolicy.roundToNearestThousand(BigDecimal.valueOf(120_999)))
            .isEqualByComparingTo("121000");
    }

    @Test
    void keepsExactThousandsUnchanged() {
        assertThat(CashRoundingPolicy.roundToNearestThousand(BigDecimal.valueOf(120_000)))
            .isEqualByComparingTo("120000");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatIllegalArgumentException().isThrownBy(
            () -> CashRoundingPolicy.roundToNearestThousand(BigDecimal.valueOf(-1))
        );
    }
}
