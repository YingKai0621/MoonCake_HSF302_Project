package com.se201411.project.mooncake;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.DeliveredOrderCompletionService;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
class DeliveryConfirmationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrderManagementService orderManagementService;

    @Autowired
    private CustomerOrderService customerOrderService;

    @Autowired
    private DeliveredOrderCompletionService completionService;

    @MockitoBean
    private AdminAccessService adminAccessService;

    private final List<Long> orderIds = new ArrayList<>();
    private Long accountId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        assertThat(jdbc.queryForObject("SELECT DB_NAME()", String.class))
            .isEqualTo("MoonCakeTestDB");
        accountId = jdbc.queryForObject("""
            INSERT INTO dbo.ACCOUNT(email, password_hash, role, status)
            OUTPUT INSERTED.account_id
            VALUES (?, 'unused-integration-fixture', 'CUSTOMER', 'ACTIVE')
            """, Long.class, "delivery-" + UUID.randomUUID() + "@example.test");
        customerId = jdbc.queryForObject("""
            INSERT INTO dbo.CUSTOMER(account_id, full_name, phone, email)
            OUTPUT INSERTED.customer_id
            VALUES (?, N'Khách kiểm thử', '0901234567', ?)
            """, Long.class, accountId, "delivery-" + UUID.randomUUID() + "@example.test");
        when(adminAccessService.requireAdmin()).thenReturn(999L);
    }

    @AfterEach
    void cleanUp() {
        for (Long orderId : orderIds) {
            jdbc.update("DELETE FROM dbo.PAYMENT WHERE order_id = ?", orderId);
            jdbc.update("DELETE FROM dbo.SALES_ORDER WHERE order_id = ?", orderId);
        }
        jdbc.update("DELETE FROM dbo.CUSTOMER WHERE customer_id = ?", customerId);
        jdbc.update("DELETE FROM dbo.ACCOUNT WHERE account_id = ?", accountId);
    }

    @Test
    void adminDeliveryAndCustomerConfirmationCompleteCodOrder() {
        String orderCode = "IT-DELIVERY-" + UUID.randomUUID();
        long orderId = createOrder(orderCode, "SHIPPING", null);
        jdbc.update("""
            INSERT INTO dbo.PAYMENT(order_id, payment_method, payment_status, amount)
            VALUES (?, 'COD', 'UNPAID', 100)
            """, orderId);

        orderManagementService.transition(orderId, OrderStatus.DELIVERED);
        customerOrderService.confirmReceived(
            orderCode,
            ShopperIdentity.forCustomer(customerId)
        );

        assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
            "SELECT payment_status FROM dbo.PAYMENT WHERE order_id = ?",
            String.class,
            orderId
        )).isEqualTo("PAID");
        assertThat(timestamp(orderId, "delivered_at")).isNotNull();
        assertThat(timestamp(orderId, "completed_at")).isNotNull();
        assertThat(timestamp(orderId, "customer_confirmed_at")).isNotNull();
    }

    @Test
    void overdueDeliveredOrderCompletesWithoutCustomerConfirmation() {
        String orderCode = "IT-AUTO-" + UUID.randomUUID();
        LocalDateTime deliveredAt = LocalDateTime.now().minusDays(4);
        long orderId = createOrder(orderCode, "DELIVERED", deliveredAt);

        int completed = completionService.completeOverdueDeliveries();

        assertThat(completed).isGreaterThanOrEqualTo(1);
        assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
        assertThat(timestamp(orderId, "completed_at")).isNotNull();
        assertThat(timestamp(orderId, "customer_confirmed_at")).isNull();
    }

    private long createOrder(
        String orderCode,
        String status,
        LocalDateTime deliveredAt
    ) {
        Long orderId = jdbc.queryForObject("""
            INSERT INTO dbo.SALES_ORDER(
                order_code,
                customer_id,
                order_type,
                buyer_name_snapshot,
                buyer_phone_snapshot,
                recipient_name,
                recipient_phone,
                shipping_address_snapshot,
                subtotal_amount,
                discount_amount,
                shipping_fee,
                total_amount,
                order_status,
                delivered_at
            )
            OUTPUT INSERTED.order_id
            VALUES (?, ?, 'RETAIL', N'Khách kiểm thử', '0901234567',
                N'Khách kiểm thử', '0901234567', N'Địa chỉ kiểm thử',
                100, 0, 0, 100, ?, ?)
            """, Long.class, orderCode, customerId, status, deliveredAt);
        orderIds.add(orderId);
        return orderId;
    }

    private String orderStatus(long orderId) {
        return jdbc.queryForObject(
            "SELECT order_status FROM dbo.SALES_ORDER WHERE order_id = ?",
            String.class,
            orderId
        );
    }

    private Timestamp timestamp(long orderId, String column) {
        return jdbc.queryForObject(
            "SELECT " + column + " FROM dbo.SALES_ORDER WHERE order_id = ?",
            Timestamp.class,
            orderId
        );
    }
}
