package com.se201411.project.mooncake.dashboard;

import com.se201411.project.mooncake.dashboard.dto.KpiView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KpiViewTest {

    @Test
    void emptyPeriodSoFarShowsPreviousValueInsteadOfMinusHundredPercent() {
        KpiView kpi = new KpiView("Doanh thu", 0, true, -100.0, "so với hôm qua", 240_000, "Hôm qua");

        assertThat(kpi.trend()).isEqualTo("waiting");
    }

    @Test
    void realDropStillShowsDown() {
        KpiView kpi = new KpiView("Doanh thu", 100_000, true, -50.0, "so với hôm qua", 200_000, "Hôm qua");

        assertThat(kpi.trend()).isEqualTo("down");
    }
}
