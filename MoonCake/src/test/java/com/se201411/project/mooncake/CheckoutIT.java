package com.se201411.project.mooncake;

import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class CheckoutIT {

    @Autowired
    private CustomerCartService customerCartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void checkoutCreatesCodOrderDeductsStockAndRejectsRepeat() {
        long customerId = createCustomer();
        ShopperIdentity customer = ShopperIdentity.forCustomer(customerId);
        long variantId = requireLong("""
            SELECT variant_id
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = N'MC-KSPM-080'
            """);
        BigDecimal unitPrice = jdbcTemplate.queryForObject("""
            SELECT unit_price
            FROM dbo.PRODUCT_VARIANT
            WHERE variant_id = ?
            """, BigDecimal.class, variantId);

        jdbcTemplate.update("""
            UPDATE dbo.INVENTORY_BALANCE
            SET available_quantity = 10,
                stock_version = stock_version + 1,
                updated_at = SYSUTCDATETIME()
            WHERE variant_id = ?
            """, variantId);
        customerCartService.addVariant(customer, variantId, 2);
        entityManager.flush();

        CheckoutPreparation preparation = checkoutService.prepareCheckout(customer);
        CheckoutForm form = new CheckoutForm();
        form.setCartId(preparation.cartId());
        form.setRecipientName("Người nhận kiểm thử");
        form.setRecipientPhone("0901234567");
        form.setProvinceCode("79");
        form.setWardCode("26740");
        form.setStreetAddress("123 Đường Trung Thu");
        form.setOrderNote("Gọi trước khi giao");

        CheckoutResult result = checkoutService.placeOrder(customer, form);
        entityManager.flush();

        Integer checkedOutCartCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.CART
            WHERE cart_id = ?
              AND customer_id = ?
              AND cart_status = 'CHECKED_OUT'
            """, Integer.class, preparation.cartId(), customerId);
        Integer orderCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.SALES_ORDER
            WHERE order_code = ?
              AND customer_id = ?
              AND order_type = 'RETAIL'
              AND order_status = 'PLACED'
            """, Integer.class, result.orderCode(), customerId);
        String paymentStatus = jdbcTemplate.queryForObject("""
            SELECT payment.payment_status
            FROM dbo.PAYMENT AS payment
            JOIN dbo.SALES_ORDER AS sales_order
                ON sales_order.order_id = payment.order_id
            WHERE sales_order.order_code = ?
              AND payment.payment_method = 'COD'
            """, String.class, result.orderCode());
        Integer stockAfter = jdbcTemplate.queryForObject("""
            SELECT available_quantity
            FROM dbo.INVENTORY_BALANCE
            WHERE variant_id = ?
            """, Integer.class, variantId);
        Integer saleMovementCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.STOCK_MOVEMENT AS movement
            JOIN dbo.SALES_ORDER AS sales_order
                ON sales_order.order_id = movement.order_id
            WHERE sales_order.order_code = ?
              AND movement.variant_id = ?
              AND movement.movement_type = 'SALE'
              AND movement.quantity_delta = -2
            """, Integer.class, result.orderCode(), variantId);
        BigDecimal total = jdbcTemplate.queryForObject("""
            SELECT total_amount
            FROM dbo.SALES_ORDER
            WHERE order_code = ?
            """, BigDecimal.class, result.orderCode());

        assertThat(checkedOutCartCount).isEqualTo(1);
        assertThat(orderCount).isEqualTo(1);
        assertThat(paymentStatus).isEqualTo("UNPAID");
        assertThat(stockAfter).isEqualTo(8);
        assertThat(saleMovementCount).isEqualTo(1);
        assertThat(total).isEqualByComparingTo(
            unitPrice.multiply(BigDecimal.valueOf(2)).add(BigDecimal.valueOf(30_000))
        );

        assertThatThrownBy(() -> checkoutService.placeOrder(customer, form))
            .isInstanceOf(ConcurrentOperationException.class);
    }

    private long createCustomer() {
        String email = "checkout-" + UUID.randomUUID() + "@mooncake.test";
        Long accountId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.ACCOUNT (email, password_hash, role, status, created_at)
            OUTPUT INSERTED.account_id
            VALUES (?, ?, 'CUSTOMER', 'ACTIVE', SYSUTCDATETIME())
            """, Long.class, email, "test-hash");

        Long customerId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.CUSTOMER (account_id, full_name, phone, email, created_at)
            OUTPUT INSERTED.customer_id
            VALUES (?, N'Checkout Integration Test', '0901234567', ?, SYSUTCDATETIME())
            """, Long.class, accountId, email);
        if (customerId == null) {
            throw new IllegalStateException("Expected customer ID");
        }
        return customerId;
    }

    private long requireLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        if (value == null) {
            throw new IllegalStateException("Expected SQL query to return one ID");
        }
        return value;
    }
}
