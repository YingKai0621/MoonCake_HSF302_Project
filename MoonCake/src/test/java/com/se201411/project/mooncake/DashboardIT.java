package com.se201411.project.mooncake;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.dto.DashboardView;
import com.se201411.project.mooncake.dashboard.service.DashboardService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
class DashboardIT {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DashboardService dashboard;
    @MockitoBean
    AdminAccessService access;

    @Test
    void everyPeriodLoadsFromOrderInventoryAndFeedbackServices() {
        assertThat(jdbc.queryForObject("SELECT DB_NAME()", String.class)).isEqualTo("MoonCakeTestDB");

        for (DashboardPeriod period : DashboardPeriod.values()) {
            DashboardView view = dashboard.dashboard(period);

            assertThat(view.period()).isEqualTo(period);
            assertThat(view.tasks()).hasSize(5);
            assertThat(view.topProducts()).hasSizeLessThanOrEqualTo(5);
        }
    }
}
