package com.se201411.project.mooncake;

import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.security.dto.AccountLockResult;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CustomerAdminService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class AdminCustomerIT {

    private static final int STARTING_STOCK = 10;
    private static final int ORDERED_QUANTITY = 2;

    @Autowired
    private CustomerAdminService customerAdminService;

    @Autowired
    private CustomerCartService customerCartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @BeforeEach
    void signInAdmin() {
        assertThat(jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class)).isEqualTo("MoonCakeTestDB");
        Long adminId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.ACCOUNT (email, password_hash, role, status)
            OUTPUT INSERTED.account_id
            VALUES (?, 'unused-integration-fixture', 'ADMIN', 'ACTIVE')
            """, Long.class, "it-admin-" + UUID.randomUUID() + "@example.test");
        when(adminAccessService.requireAdmin()).thenReturn(adminId);
    }

    @Test
    void lockCancelsUnpaidCodOrderRestoresStockOnceAndUnlockReactivates() {
        long customerId = createCustomer();
        long variantId = jdbcTemplate.queryForObject("""
            SELECT variant_id
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = N'MC-KSPM-080'
            """, Long.class);
        jdbcTemplate.update("""
            UPDATE dbo.INVENTORY_BALANCE
            SET available_quantity = ?,
                stock_version = stock_version + 1,
                updated_at = SYSUTCDATETIME()
            WHERE variant_id = ?
            """, STARTING_STOCK, variantId);
        String orderCode = placeCodOrder(customerId, variantId);
        assertThat(stockOf(variantId)).isEqualTo(STARTING_STOCK - ORDERED_QUANTITY);

        AccountLockResult result = customerAdminService.lock(customerId, true);
        entityManager.flush();

        assertThat(result.cancelledCount()).isEqualTo(1);
        assertThat(result.manualOrderCodes()).isEmpty();
        assertThat(accountStatusOf(customerId)).isEqualTo("DISABLED");
        assertThat(orderStatusOf(orderCode)).isEqualTo("CANCELLED");
        assertThat(stockOf(variantId)).isEqualTo(STARTING_STOCK);
        Integer restockMovements = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.STOCK_MOVEMENT AS movement
            JOIN dbo.SALES_ORDER AS sales_order
                ON sales_order.order_id = movement.order_id
            WHERE sales_order.order_code = ?
              AND movement.quantity_delta > 0
            """, Integer.class, orderCode);
        assertThat(restockMovements).isEqualTo(1);

        assertThatThrownBy(() -> customerAdminService.lock(customerId, true))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Tài khoản đã bị khóa trước đó.");

        customerAdminService.unlock(customerId);
        entityManager.flush();

        assertThat(accountStatusOf(customerId)).isEqualTo("ACTIVE");
        assertThat(orderStatusOf(orderCode)).isEqualTo("CANCELLED");
    }

    private String placeCodOrder(long customerId, long variantId) {
        ShopperIdentity customer = ShopperIdentity.forCustomer(customerId);
        customerCartService.addVariant(customer, variantId, ORDERED_QUANTITY);
        entityManager.flush();

        CheckoutPreparation preparation = checkoutService.prepareCheckout(customer);
        CheckoutForm form = new CheckoutForm();
        form.setCartId(preparation.cartId());
        form.setRecipientName("Người nhận kiểm thử");
        form.setRecipientPhone("0901234567");
        form.setProvinceCode("79");
        form.setWardCode("26740");
        form.setStreetAddress("123 Đường Trung Thu");

        CheckoutResult result = checkoutService.placeOrder(customer, form);
        entityManager.flush();
        return result.orderCode();
    }

    private long createCustomer() {
        String email = "admin-customer-" + UUID.randomUUID() + "@mooncake.test";
        Long accountId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.ACCOUNT (email, password_hash, role, status, created_at)
            OUTPUT INSERTED.account_id
            VALUES (?, 'test-hash', 'CUSTOMER', 'ACTIVE', SYSUTCDATETIME())
            """, Long.class, email);
        return jdbcTemplate.queryForObject("""
            INSERT INTO dbo.CUSTOMER (account_id, full_name, phone, email, created_at)
            OUTPUT INSERTED.customer_id
            VALUES (?, N'Khách bị khóa', '0901234567', ?, SYSUTCDATETIME())
            """, Long.class, accountId, email);
    }

    private int stockOf(long variantId) {
        return jdbcTemplate.queryForObject("""
            SELECT available_quantity
            FROM dbo.INVENTORY_BALANCE
            WHERE variant_id = ?
            """, Integer.class, variantId);
    }

    private String accountStatusOf(long customerId) {
        return jdbcTemplate.queryForObject("""
            SELECT account.status
            FROM dbo.ACCOUNT AS account
            JOIN dbo.CUSTOMER AS customer
                ON customer.account_id = account.account_id
            WHERE customer.customer_id = ?
            """, String.class, customerId);
    }

    private String orderStatusOf(String orderCode) {
        return jdbcTemplate.queryForObject("""
            SELECT order_status
            FROM dbo.SALES_ORDER
            WHERE order_code = ?
            """, String.class, orderCode);
    }
}
