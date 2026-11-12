package com.se201411.project.mooncake;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.security.dto.AdminAccountForm;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.AdminAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class AdminAccountIT {

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    private long superAdminId;

    @BeforeEach
    void setUp() {
        assertThat(jdbc.queryForObject("SELECT DB_NAME()", String.class)).isEqualTo("MoonCakeTestDB");
        Long lowestAdmin = jdbc.queryForObject("SELECT MIN(account_id) FROM dbo.ACCOUNT WHERE role = 'ADMIN'", Long.class);
        if (lowestAdmin == null) {
            lowestAdmin = jdbc.queryForObject("""
                    INSERT INTO dbo.ACCOUNT(email, password_hash, role, status)
                    OUTPUT INSERTED.account_id VALUES (?, 'unused-integration-fixture', 'ADMIN', 'ACTIVE')
                    """, Long.class, "it-super-" + UUID.randomUUID() + "@example.test");
        }
        superAdminId = lowestAdmin;
    }

    @Test
    void superAdminCreatesAndLocksAnAdminWhoCanNoLongerSignIn() {
        when(adminAccessService.requireAdmin()).thenReturn(superAdminId);
        String email = "it-staff-" + UUID.randomUUID() + "@example.test";
        AdminAccountForm form = new AdminAccountForm();
        form.setEmail(email);
        form.setPassword("matkhau123");

        adminAccountService.create(form);
        long staffId = jdbc.queryForObject("SELECT account_id FROM dbo.ACCOUNT WHERE email = ?", Long.class, email);
        assertThat(userDetailsService.loadUserByUsername(email).isEnabled()).isTrue();

        adminAccountService.lock(staffId);

        String status = jdbc.queryForObject("SELECT status FROM dbo.ACCOUNT WHERE account_id = ?", String.class, staffId);
        assertThat(status).isEqualTo("DISABLED");
        UserDetails locked = userDetailsService.loadUserByUsername(email);
        assertThat(locked.isEnabled()).isFalse();
    }

    @Test
    void newAdminCannotManageOtherAdmins() {
        when(adminAccessService.requireAdmin()).thenReturn(superAdminId);
        String email = "it-staff-" + UUID.randomUUID() + "@example.test";
        AdminAccountForm form = new AdminAccountForm();
        form.setEmail(email);
        form.setPassword("matkhau123");
        adminAccountService.create(form);
        long staffId = jdbc.queryForObject("SELECT account_id FROM dbo.ACCOUNT WHERE email = ?", Long.class, email);

        when(adminAccessService.requireAdmin()).thenReturn(staffId);

        assertThatThrownBy(() -> adminAccountService.lock(superAdminId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Chỉ super admin");
    }
}
